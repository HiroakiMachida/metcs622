package edu.bu.metcs662.machidahiroaki_final_project;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller class for RESTful service.
 * 
 * @author hiroakimachida
 *
 */
@RestController
public class RequestController {

	/**
	 * Request controller for Brute Force search.
	 * 
	 * @param searchType searchType
	 * @param searchWord searchWord
	 * @return searchResult
	 * @throws Exception
	 */
	@RequestMapping("/getBruteForceResult")
	public Request getBruteForceResult(
			@RequestParam(value = "searchType", defaultValue = "HeartRate") String searchType,
			@RequestParam(value = "searchWord", defaultValue = "\"bpm\":75") String searchWord) throws Exception {
		String result = HWUtil.queryByBruteForce(searchType, "0", searchWord, 5);
		return new Request(result);
	}

	/**
	 * Request controller for Lucene.
	 * 
	 * @param searchType searchType
	 * @param searchWord searchWord
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/getLuceneResult")
	public Request getLuceneResult(@RequestParam(value = "searchType", defaultValue = "HeartRate") String searchType,
			@RequestParam(value = "searchWord", defaultValue = "\"bpm\":75") String searchWord) throws Exception {
		String result = HWUtil.queryByLucene(searchType, "0", searchWord, 5);
		return new Request(result);
	}

	/**
	 * Request controller for MongoDB.
	 * 
	 * @param searchType  searchType
	 * @param searchField searchField
	 * @param searchWord  searchWord
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/getMongoDBResult")
	public Request getLuceneResult(
			@RequestParam(value = "searchType", defaultValue = "sa/lightsensor") String searchType,
			@RequestParam(value = "searchField", defaultValue = "data") String searchField,
			@RequestParam(value = "searchWord", defaultValue = "light") String searchWord) throws Exception {
		String result = HWUtil.queryByMongoDB(searchType, "0", searchField, searchWord, 5);
		return new Request(result);
	}

	/**
	 * Request controller for MongoDB.
	 * 
	 * @param searchType  searchType
	 * @param searchField searchField
	 * @param searchWord  searchWord
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/getMySQLResult")
	public Request getMySQLResult(@RequestParam(value = "searchType", defaultValue = "HeartRate") String searchType,
			@RequestParam(value = "searchField", defaultValue = "sensor_data.bpm") String searchField,
			@RequestParam(value = "searchWord", defaultValue = "75") String searchWord) throws Exception {
		String result = HWUtil.queryByMySQL(searchType, "0", searchField, searchWord, 5);
		return new Request(result);
	}

	/**
	 * Request controller for images.
	 * 
	 * @param request  request
	 * @param response response
	 * @param type     type
	 * @throws IOException
	 */
	@RequestMapping(value = "/image", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
	public void getImage(HttpServletRequest request, HttpServletResponse response,
			@RequestParam(value = "type") String type) throws IOException {
		File downloadFile = new File("src/main/resources/image/" + type + ".jpg");
		System.out.println(downloadFile.getCanonicalPath());
		FileInputStream inputStream = new FileInputStream(downloadFile);
		response.setContentType(MediaType.IMAGE_JPEG_VALUE);
		StreamUtils.copy(inputStream, response.getOutputStream());

	}
}
