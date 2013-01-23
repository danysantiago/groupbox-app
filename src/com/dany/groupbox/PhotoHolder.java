package com.dany.groupbox;

import java.io.File;

public class PhotoHolder {
	private File file;
	private Long dateAdded;

	public PhotoHolder(File file, Long dateAdded) {
		this.file = file;
		this.dateAdded = dateAdded;
	}

	public Long getDateAdded() {
		return dateAdded;
	}

	public File getFile() {
		return file;
	}

}
