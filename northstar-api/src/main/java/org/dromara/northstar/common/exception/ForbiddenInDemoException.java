package org.dromara.northstar.common.exception;

public class ForbiddenInDemoException extends OperationException{

	/**
	 * 
	 */
	private static final long serialVersionUID = -9129148014838270603L;

	public ForbiddenInDemoException() {
		super("error.operation_forbidden_in_demo");
	}

}
