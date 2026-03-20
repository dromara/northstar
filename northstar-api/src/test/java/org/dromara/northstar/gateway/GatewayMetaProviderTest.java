package org.dromara.northstar.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dromara.northstar.common.GatewaySettings;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.FieldType;
import org.dromara.northstar.common.model.ComponentField;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;
import org.dromara.northstar.meta.GatewayMetaProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GatewayMetaProviderTest {

	 private GatewayMetaProvider provider;
	    private GatewaySettings mockSettings = new DummyGatewaySettings();
	    private GatewayFactory mockFactory;
	    private ChannelType mockChannelType = ChannelType.CTP;

	    @BeforeEach
	    void setUp() {
	    	mockFactory = mock(GatewayFactory.class);
	    	when(mockFactory.channelType()).thenReturn(mockChannelType);
	        provider = new GatewayMetaProvider(List.of(mockFactory));
	    }

	    @Test
	    void testGetFactoryWithSetting() {
	        GatewayFactory factory = provider.getFactory(mockChannelType);
	        assertNotNull(factory);
	        assertEquals(mockFactory, factory);
	    }

	    @Test
	    void testAdd() {
	        Map<ChannelType, Collection<ComponentField>> settings = provider.getSettings();
	        GatewayFactory factory = provider.getFactory(mockChannelType);
	        
	        assertNotNull(settings);
	        assertNotNull(factory);
	        assertEquals(mockFactory, factory);
	    }
	    
	    @Test
	    void testResolve() throws Exception {
	    	Map<String, ComponentField> fieldMap = ((DynamicParams)mockSettings).getMetaInfo().stream().collect(Collectors.toMap(ComponentField::getName, f -> f));
	    	
	    	assertThat(fieldMap.get("openSpreadRate").getValue()).isEqualTo("1.0");
	    	assertThat(fieldMap.get("indicatorSymbol").getValue()).isEqualTo("test1");
	    	
	    	fieldMap.get("openSpreadRate").setValue(5D);
	    	fieldMap.get("indicatorSymbol").setValue("sss");
	    	
	    	DummyGatewaySettings settings = (DummyGatewaySettings) ((DynamicParams)mockSettings).resolveFromSource(fieldMap.values());
	    	assertThat(settings.indicatorSymbol).isEqualTo("sss");
	    	assertThat(settings.openSpreadRate).isEqualTo(5D);
	    }

	    @Test
	    void testAvailableChannel() {
	        List<ChannelType> availableChannels = provider.availableChannel();
	        assertNotNull(availableChannels);
	        assertFalse(availableChannels.isEmpty());
	        assertTrue(availableChannels.contains(mockChannelType));
	    }
	    
	    class DummyGatewaySettings extends DynamicParams implements GatewaySettings{
	    	
	    	@Setting(label="开仓价差率%", order=0, type = FieldType.NUMBER)
			private double openSpreadRate = 1;
			
			@Setting(label="指标合约", order=0)
			private String indicatorSymbol = "test1";
			
	    }
}
