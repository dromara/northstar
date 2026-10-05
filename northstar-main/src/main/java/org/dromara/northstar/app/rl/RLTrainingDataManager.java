package org.dromara.northstar.app.rl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.gateway.IContract;

import lombok.extern.slf4j.Slf4j;

/**
 * RL 训练数据管理器：行情数据从数据源一次性拉取并落地为本地 CSV，训练阶段只读 CSV。
 *
 * 目录布局：{@code <data-root>/<taskId>/<unifiedSymbol>.csv}（每合约一个文件）。
 * 写入先落 {@code .tmp} 再原子改名，保证「文件存在即数据完整」，
 * 程序重启后已落盘的合约可直接跳过加载（断点续传）。
 */
@Slf4j
public class RLTrainingDataManager {

	private static final String CSV_HEADER = String.join(",",
			"actionDay", "actionTime", "tradingDay", "actionTimestamp",
			"open", "high", "low", "close",
			"openInterest", "openInterestDelta", "volume", "volumeDelta",
			"turnover", "turnoverDelta", "preOpenInterest", "preClosePrice", "preSettlePrice");

	private static final String TMP_SUFFIX = ".tmp";

	private final Path dataRoot;

	public RLTrainingDataManager(Path dataRoot) {
		this.dataRoot = dataRoot;
	}

	/** 任务的最终 CSV 存在即视为该合约数据已就绪（.tmp 半截文件不算） */
	public boolean dataExists(String taskId, String unifiedSymbol) {
		return Files.isRegularFile(csvFile(taskId, unifiedSymbol));
	}

	/**
	 * 删除任务的整个数据目录（任务删除时调用；目录不存在为正常情况）。
	 * 删除失败抛异常而非静默吞掉：调用方据此中止后续操作，避免记录已删、数据残留的不一致状态。
	 *
	 * @param taskName 任务展示名（模型名+版本），仅用于日志/异常信息，路径仍按 taskId 定位
	 */
	public void deleteTaskData(String taskId, String taskName) {
		Path taskDir = dataRoot.resolve(taskId);
		if (!Files.isDirectory(taskDir)) {
			return;
		}
		try (var stream = Files.walk(taskDir)) {
			for (Path p : stream.sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(p);
			}
		} catch (IOException e) {
			throw new UncheckedIOException("清理 RL 任务 " + taskName + " 数据目录失败: " + taskDir, e);
		}
	}

	/**
	 * 从数据源按周分页拉取 [startDate, 今天] 的 M1 数据并写入 CSV（原子改名）。
	 *
	 * @param taskName 任务展示名（模型名+版本），仅用于日志，路径与文件仍按 taskId 命名
	 * @return 落盘的 bar 数
	 */
	public int fetchAndSave(IContract ic, LocalDate startDate, String taskId, String taskName) throws IOException {
		List<Bar> bars = fetchBarsAscending(ic, startDate);
		Path tmp = csvFile(taskId, ic.contract().unifiedSymbol() + TMP_SUFFIX);
		Path target = csvFile(taskId, ic.contract().unifiedSymbol());
		Files.createDirectories(tmp.getParent());
		try (BufferedWriter writer = Files.newBufferedWriter(tmp)) {
			writer.write(CSV_HEADER);
			writer.newLine();
			for (Bar bar : bars) {
				writer.write(toCsvLine(bar));
				writer.newLine();
			}
		}
		try {
			Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
		}
		log.info("RL 任务 {} 合约 {} 数据落盘：{} 根 M1 bar -> {}", taskName, ic.contract().unifiedSymbol(), bars.size(), target);
		return bars.size();
	}

	/** 读取 CSV 还原为时间升序的 Bar 列表（interval=M1、channelType=PLAYBACK） */
	public List<Bar> load(String taskId, Contract contract) throws IOException {
		Path csv = csvFile(taskId, contract.unifiedSymbol());
		if (!Files.isRegularFile(csv)) {
			throw new IllegalStateException("合约 %s 数据文件不存在，请先加载数据: %s".formatted(contract.unifiedSymbol(), csv));
		}
		List<Bar> bars = new ArrayList<>();
		try (BufferedReader reader = Files.newBufferedReader(csv)) {
			String line = reader.readLine();	// 表头
			while ((line = reader.readLine()) != null) {
				if (!line.isBlank()) {
					bars.add(fromCsvLine(line, contract));
				}
			}
		}
		bars.sort(Comparator.comparingLong(Bar::actionTimestamp));
		return bars;
	}

	/** 按周分页加载 [startDate, 今天] 的 M1 数据，返回时间升序列表 */
	private List<Bar> fetchBarsAscending(IContract ic, LocalDate startDate) {
		List<Bar> all = new ArrayList<>();
		LocalDate cursor = startDate;
		LocalDate today = LocalDate.now();
		int emptyStreak = 0;
		IDataSource dataSource = ic.dataSource();
		while (!cursor.isAfter(today) && emptyStreak < 4) {
			List<Bar> page = dataSource.getData(ic.contract(), Interval.M1, cursor, cursor.plusDays(6));
			if (page == null || page.isEmpty()) {
				emptyStreak++;
			} else {
				emptyStreak = 0;
				all.addAll(page);
			}
			cursor = cursor.plusWeeks(1);
		}
		all.sort(Comparator.comparingLong(Bar::actionTimestamp));
		return all;
	}

	private Path csvFile(String taskId, String fileName) {
		return dataRoot.resolve(taskId).resolve(fileName + (fileName.endsWith(TMP_SUFFIX) ? "" : ".csv"));
	}

	private static String toCsvLine(Bar b) {
		return String.join(",",
				b.actionDay().toString(),
				b.actionTime().toString(),
				b.tradingDay().toString(),
				Long.toString(b.actionTimestamp()),
				Double.toString(b.openPrice()),
				Double.toString(b.highPrice()),
				Double.toString(b.lowPrice()),
				Double.toString(b.closePrice()),
				Long.toString(b.openInterest()),
				Integer.toString(b.openInterestDelta()),
				Double.toString(b.volume()),
				Double.toString(b.volumeDelta()),
				Double.toString(b.turnover()),
				Double.toString(b.turnoverDelta()),
				Long.toString(b.preOpenInterest()),
				Double.toString(b.preClosePrice()),
				Double.toString(b.preSettlePrice()));
	}

	private static Bar fromCsvLine(String line, Contract contract) {
		String[] f = line.split(",", -1);
		if (f.length != 17) {
			throw new IllegalStateException("CSV 行字段数异常（期望 17，实际 " + f.length + "）: " + line);
		}
		return Bar.builder()
				.gatewayId(contract.gatewayId())
				.contract(contract)
				.actionDay(LocalDate.parse(f[0]))
				.actionTime(LocalTime.parse(f[1]))
				.tradingDay(LocalDate.parse(f[2]))
				.actionTimestamp(Long.parseLong(f[3]))
				.openPrice(Double.parseDouble(f[4]))
				.highPrice(Double.parseDouble(f[5]))
				.lowPrice(Double.parseDouble(f[6]))
				.closePrice(Double.parseDouble(f[7]))
				.openInterest(Long.parseLong(f[8]))
				.openInterestDelta(Integer.parseInt(f[9]))
				.volume(Double.parseDouble(f[10]))
				.volumeDelta(Double.parseDouble(f[11]))
				.turnover(Double.parseDouble(f[12]))
				.turnoverDelta(Double.parseDouble(f[13]))
				.preOpenInterest(Long.parseLong(f[14]))
				.preClosePrice(Double.parseDouble(f[15]))
				.preSettlePrice(Double.parseDouble(f[16]))
				.interval(Interval.M1)
				.unstable(false)
				.channelType(ChannelType.PLAYBACK)
				.build();
	}
}
