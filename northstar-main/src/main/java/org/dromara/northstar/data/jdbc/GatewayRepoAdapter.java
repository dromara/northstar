package org.dromara.northstar.data.jdbc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.data.IGatewayRepository;
import org.dromara.northstar.data.jdbc.model.GatewayDescriptionDO;

public class GatewayRepoAdapter implements IGatewayRepository{
	
	private GatewayDescriptionRepository delegate;
	
	public GatewayRepoAdapter(GatewayDescriptionRepository delegate) {
		this.delegate = delegate;
	}

	@Override
	public void insert(GatewayDescription gatewayDescription) {
		if(delegate.existsById(gatewayDescription.getGatewayId())) {
			throw new IllegalStateException("已存在同名网关，不能重复创建");
		}
		delegate.save(GatewayDescriptionDO.convertFrom(gatewayDescription));
	}

	@Override
	public void save(GatewayDescription gatewayDescription) {
		delegate.save(GatewayDescriptionDO.convertFrom(gatewayDescription));
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
		return list;
	}

	@Override
	public GatewayDescription findById(String gatewayId) {
		GatewayDescriptionDO gdDo = delegate.findById(gatewayId).orElseThrow();
		return gdDo.convertTo();
	}


}
