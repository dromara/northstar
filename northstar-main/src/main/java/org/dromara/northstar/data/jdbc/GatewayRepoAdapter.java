package org.dromara.northstar.data.jdbc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dromara.northstar.common.exception.NoSuchGatewayException;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.data.IGatewayRepository;
import org.dromara.northstar.data.jdbc.entity.GatewayDescriptionDO;
import org.dromara.northstar.support.utils.CodecUtils;

import com.alibaba.fastjson2.JSON;

public class GatewayRepoAdapter implements IGatewayRepository{
	
	private GatewayDescriptionRepository delegate;
	
	public GatewayRepoAdapter(GatewayDescriptionRepository delegate) {
		this.delegate = delegate;
	}

	@Override
	public void save(GatewayDescription gatewayDescription) {
		Object settings = gatewayDescription.getSettings();
		gatewayDescription.setSettings(CodecUtils.encrypt(JSON.toJSONString(settings)));
		delegate.save(GatewayDescriptionDO.convertFrom(gatewayDescription));
		gatewayDescription.setSettings(settings);
	}

	@Override
	public void deleteById(String gatewayId) {
		delegate.deleteById(gatewayId);
	}

	@Override
	public List<GatewayDescription> findAll() {
		List<GatewayDescription> list = new ArrayList<>();
		Iterator<GatewayDescriptionDO> itResults = delegate.findAll().iterator();
		while(itResults.hasNext()) {
			GatewayDescriptionDO obj = itResults.next();
			list.add(obj.convertTo());
		}
		return list.stream()
				.map(gd -> {
					String encrypted = gd.getSettings().toString();
					gd.setSettings(JSON.parse(CodecUtils.decrypt(encrypted)));
					return gd;
				})
				.toList();
	}

	@Override
	public GatewayDescription findById(String gatewayId) {
		GatewayDescriptionDO gdDo = delegate.findById(gatewayId).orElseThrow(() -> new NoSuchGatewayException(gatewayId));
		GatewayDescription gd = gdDo.convertTo();
		String encrypted = gd.getSettings().toString();
		gd.setSettings(JSON.parse(CodecUtils.decrypt(encrypted)));
		return gd;
	}


}
