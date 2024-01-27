package org.dromara.northstar.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.dromara.northstar.common.GatewaySettings;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.FieldType;
import org.dromara.northstar.common.model.ComponentField;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GatewayMetaProviderTest {

	 private GatewayMetaProvider provider;
	    private GatewaySettings mockSettings = new DummyGatewaySettings();
	    private GatewayFactory mockFactory;
	    private ChannelType mockChannelType = ChannelType.CTP;

	    @BeforeEach
	    void setUp() {
	        provider = new GatewayMetaProvider();
	        mockFactory = mock(GatewayFactory.class);
	    }

	    @Test
	    void testGetFactoryWithoutSettingShouldThrowException() {
	    	assertThrows(IllegalStateException.class, () -> {	    		
	    		provider.getFactory(mockChannelType);
	    	});
	    }

	    @Test
	    void testGetFactoryWithSetting() {
	        provider.add(mockChannelType, null, mockFactory);
	        GatewayFactory factory = provider.getFactory(mockChannelType);
	        assertNotNull(factory);
	        assertEquals(mockFactory, factory);
	    }

	    @Test
	    void testAdd() {
	        provider.add(mockChannelType, mockSettings, mockFactory);
	        Collection<ComponentField> settings = provider.getSettings(mockChannelType);
	        GatewayFactory factory = provider.getFactory(mockChannelType);
	        
	        assertNotNull(settings);
	        assertNotNull(factory);
	        assertEquals(mockFactory, factory);
	    }
	    
	    @Test
	    void testResolve() throws Exception {
	    	Map<String, ComponentField> fieldMap = ((DynamicParams)mockSettings).getMetaInfo();
	    	
	    	assertThat(fieldMap.get("openSpreadRate").getValue()).isEqualTo(1D);
	    	assertThat(fieldMap.get("indicatorSymbol").getValue()).isEqualTo("test1");
	    	
	    	fieldMap.get("openSpreadRate").setValue(5D);
	    	fieldMap.get("indicatorSymbol").setValue("sss");
	    	
	    	DummyGatewaySettings settings = (DummyGatewaySettings) ((DynamicParams)mockSettings).resolveFromSource(fieldMap);
	    	assertThat(settings.indicatorSymbol).isEqualTo("sss");
	    	assertThat(settings.openSpreadRate).isEqualTo(5D);
	    }

	    @Test
	    void testAvailableChannel() {
	        provider.add(mockChannelType, null, mockFactory);
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
