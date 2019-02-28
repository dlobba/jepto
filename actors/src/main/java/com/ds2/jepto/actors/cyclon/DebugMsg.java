package com.ds2.jepto.actors.cyclon;

import java.io.Serializable;

public class DebugMsg implements Serializable {
	public enum DebugType {
		PRINT_CACHE,
		TRACE_CACHE,
		DIE;
	}
	
	private DebugType type;
	
	public DebugMsg(DebugType type) {
		this.type = type;
	}
	
	public DebugMsg() {
		this.type = DebugType.PRINT_CACHE;
	}
	
	public DebugType getType() {
		return this.type;
	}
}

