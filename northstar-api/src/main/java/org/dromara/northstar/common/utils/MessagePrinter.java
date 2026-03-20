package org.dromara.northstar.common.utils;

import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.TextFormat;

public class MessagePrinter {
	
	private MessagePrinter() {}
	
	public static String print(MessageOrBuilder pbObject) {
		return TextFormat.printer().escapingNonAscii(false).printToString(pbObject);
	}
	
	public static String shortPrint(MessageOrBuilder pbObject) {
		return TextFormat.printer().escapingNonAscii(false).shortDebugString(pbObject);
	}
}
