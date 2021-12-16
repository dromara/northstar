package tech.quantit.northstar.common.utils;

import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.TextFormat;

public interface MessagePrinter {

	static String print(MessageOrBuilder pbObject) {
		return TextFormat.printer().escapingNonAscii(false).printToString(pbObject);
	}
}
