package com.tencent.polaris.plugins.configuration.connector.localfile;

/**
 * @author rod.xu
 * @date 2022/9/28 10:29 上午
 */
public class ConfigFileChange {

	public ConfigFileChange(ChangeType changeType, String fileName) {
		this.fileName = fileName;
		this.changeType = changeType;
	}

	private String fileName;

	private ChangeType changeType;

	public enum ChangeType {

		/**
		 * create file
		 */
		CREATE,
		UPDATE,
		DELETE
	}

	public String getFileName() {
		return fileName;
	}

	public ChangeType getChangeType() {
		return changeType;
	}
}
