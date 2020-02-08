package edu.bu.metcs662.machidahiroaki_final_project;

/**
 * Request class to return response to user.
 * 
 * @author hiroakimachida
 *
 */
public class Request {

	private final String content;

	public Request(String content) {
		this.content = content;
	}

	public String getContent() {
		return content;
	}
}
