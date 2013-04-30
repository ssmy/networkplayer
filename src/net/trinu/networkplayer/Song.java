package net.trinu.networkplayer;

public class Song {
	private String path;
	private boolean isCached;

	public Song(String path) {
		this.path = path;
		isCached = false;
	}
	
	public String getPath() {
		return path;
	}
	
	public boolean isCached() {
		return isCached;
	}
	
	public void setCached(boolean cached) {
		isCached = cached;
	}

}
