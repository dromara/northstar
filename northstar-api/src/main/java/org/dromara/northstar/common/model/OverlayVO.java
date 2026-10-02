package org.dromara.northstar.common.model;

import java.util.List;

import com.alibaba.fastjson2.JSONObject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OverlayVO {

	private int currentStep;
    private List<Point> points;
    private String name;
    private int totalStep;
    private boolean lock;
    private boolean visible;
    private int zLevel;
    private boolean needDefaultPointFigure;
    private boolean needDefaultXAxisFigure;
    private boolean needDefaultYAxisFigure;
    private String mode;
    private int modeSensitivity;
    private JSONObject extendData;
    private JSONObject styles;
    private String paneId;
    private String id;
    private String groupId;

    @Data
    public static class Point {
        private long timestamp;
        private int dataIndex;
        private double value;
    }

}
