package edu.bu.metcs662.machidahiroaki_final_project;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.annotation.PreDestroy;

import java.sql.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.zeroturnaround.zip.ZipUtil;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.bson.json.JsonParseException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * The util class for the main class and controller.
 * 
 * @author hiroakimachida
 */
public class HWUtil {
	/**
	 * MSEC!! Sensor types to download, make queries, and test.
	 */
	final static String[] SENSOR_TYPES = { "SA/LightSensor", "ActivFit", "HeartRate", };

	/**
	 * Numbers of days to do performance test.
	 */
	final static String[] DAYS = { "0", "1", "5", "10", "20", "30", "50", "70", "100" };

	/**
	 * Number of performance test for each sensor type and number of days. This is
	 * required to get accurate results.
	 */
	final static Integer SIMULATION_COUNT = 100;

	/**
	 * Enum for search method.
	 */
	protected enum Method {
		BRUTE_FORCE, LUCENE_INDEX, MONGODB, MYSQL
	};

	/**
	 * MySQL Connection.
	 */
	static Connection mySQLConnection;

	/**
	 * MongoDB Client.
	 */
	static MongoClient mongoClient;

	/**
	 * Download data and unzip it
	 */
	public static void getData() throws IOException {
		/**
		 * Download the data for this sample user.
		 */
		URL url = new URL(
				"https://drive.google.com/uc?authuser=0&id=146QjQ9a8TdXiMZg6sqBvuo77veTSs_6T&export=download");
		File downloaded = new File("data.zip");
		if (!downloaded.exists()) {
			FileUtils.copyURLToFile(url, downloaded);
			System.out.println(downloaded.toString() + " downloaded.");
		}

		/**
		 * Unzip the file downloaded.
		 */
		File destination = new File("data");
		List<File> files;
		if (!destination.exists()) {
			ZipUtil.unpack(downloaded, destination);
			files = (List<File>) FileUtils.listFiles(destination, new String[] { "zip" }, true);
			for (File file : files) {
				if (!file.isHidden())
					ZipUtil.unpack(file, new File(FilenameUtils.removeExtension(file.getCanonicalPath())));
			}
			System.out.println(destination.toString() + " unzipped.");
		}
//        FileUtils.deleteQuietly(downloaded);
	}

	/**
	 * Merge unzipped files into one file for each type.
	 */
	public static void merge() throws IOException {
		// Get unzipped files.
		File destination = new File("data");
		List<File> files = (List<File>) FileUtils.listFiles(destination, TrueFileFilter.INSTANCE,
				TrueFileFilter.INSTANCE);
		// Merge files for each sensor type and number of days.
		for (String type : SENSOR_TYPES) {
			for (String days : DAYS) {
				String filename = days == "0" ? type : type + "." + days;
				File merged = new File(filename);
				if (!merged.exists()) {
					// Choose data files.
					String regex;
					switch (type) {
					case "SA/LightSensor":
						regex = "^.*" + type + ".*\\.txt$";
						break;
					default:
						regex = "^(?!.*/SA/).*(" + type + ").*\\.txt$";
					}
					Pattern pattern = Pattern.compile(regex);
					FileOutputStream out = FileUtils.openOutputStream(merged, true);
					// Limit number of days to merge for performance test.
					Integer count = 0;
					Integer maxFiles = Integer.valueOf(days) == 0 ? Integer.MAX_VALUE : Integer.valueOf(days);
					for (File f : files) {
						if (maxFiles <= count)
							break;
						if (pattern.matcher(f.getCanonicalPath()).matches()) {
							// Merge an unzipped file into a merged file.
							FileInputStream in = FileUtils.openInputStream(f);
							IOUtils.copy(in, out);
							count++;
							in.close();
						}
					}
					out.close();
					System.out.println(merged.toString() + " created.");
				}
			}
		}
//		FileUtils.deleteDirectory(destination);
	}

	/**
	 * Create lucene indices for each sensor type and number of days.
	 */
	public static void createIndices() throws Exception {
		for (String type : SENSOR_TYPES) {
			for (String days : DAYS) {
				// 0. Specify the analyzer for tokenizing text.
				// The same analyzer should be used for indexing and searching
				StandardAnalyzer analyzer = new StandardAnalyzer();

				// 1. create the index
				// Index file name.
				String indexName = days == "0" ? type + ".idx" : type + "." + days + ".idx";
				Path indexPath = Paths.get(indexName);
				if (!Files.exists(indexPath)) {
					Directory index = FSDirectory.open(indexPath);
					IndexWriterConfig config = new IndexWriterConfig(analyzer);
					IndexWriter w = new IndexWriter(index, config);
					// Merged files
					String filename = days == "0" ? type : type + "." + days;
					BufferedReader r = new BufferedReader(new FileReader(filename));
					String data;
					// Add records of merged files to index files.
					while ((data = r.readLine()) != null) {
						org.apache.lucene.document.Document doc = new Document();
						doc.add(new TextField("data", data, Field.Store.YES));
						w.addDocument(doc);
					}
					r.close();
					w.close();
					System.out.println(indexName + " created.");
				}
			}
		}
	}

	/**
	 * Import the data into MongoDB.
	 */
	public static void importDataMongoDB() throws IOException {
		// Initiate a connection
		MongoClient mongoClient = new MongoClient();
		MongoDatabase dbObj = mongoClient.getDatabase("machidahiroaki");

		// Import the data into MongoDB for each sensor type
		for (String type : SENSOR_TYPES) {
			for (String days : DAYS) {
				MongoCollection<org.bson.Document> col = dbObj.getCollection(type.toLowerCase() + "." + days);
				if (0 < col.estimatedDocumentCount())
					continue;
				String filename = days == "0" ? type : type + "." + days;
				BufferedReader reader = new BufferedReader(new FileReader(filename));
				String json;
				while ((json = reader.readLine()) != null) {
					try {
						col.insertOne(org.bson.Document.parse(json));
					} catch (JsonParseException e) {
						col.insertOne(new org.bson.Document("data", json));
					}
				}
				System.out.println("MongoDB " + type + "." + days + " imported.");
				reader.close();
//			col.drop();
			}
		}
		mongoClient.close();
	}

	/**
	 * Import the data into MySQL.
	 */
	public static void importDataMySQL() throws IOException {
		try {
			Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306", "root", "root");
			Statement stmt = con.createStatement();
			stmt.executeUpdate("CREATE DATABASE met622");
			con.close();
		} catch (SQLException ex) {
			System.out.println("Database already exists.");
		}
		for (String days : DAYS) {
			try {
				Connection con = DriverManager.getConnection(
						"jdbc:mysql://localhost:3306/met622?useTimezone=true&serverTimezone=UTC", "root", "root");

				Statement stmt = con.createStatement();
				stmt.executeUpdate(
						"CREATE TABLE `met622`.`SA/LightSensor" + "." + days + "` (`data` VARCHAR(1000) NOT NULL);");
				stmt.executeUpdate("CREATE TABLE `met622`.`ActivFit" + "." + days
						+ "` (`sensor_name` VARCHAR(100) NULL,`timestamp.start_date` VARCHAR(100) NULL,`timestamp.end_time` VARCHAR(100) NULL,`sensor_data.activity` VARCHAR(100) NULL,`sensor_data.duration` VARCHAR(100) NULL);");
				stmt.executeUpdate("CREATE TABLE `met622`.`HeartRate" + "." + days
						+ "` (`sensor_name` VARCHAR(100) NULL,`timestamp` VARCHAR(100) NULL,`sensor_data.bpm` VARCHAR(100) NULL);");
				con.close();
			} catch (SQLException ex) {
				System.out.println("Tables already exist.");
			}
			try {
				Connection con = DriverManager.getConnection(
						"jdbc:mysql://localhost:3306/met622?useTimezone=true&serverTimezone=UTC", "root", "root");
				Statement stmt = con.createStatement();

				for (String type : SENSOR_TYPES) {
					ResultSet rs = stmt
							.executeQuery("select count(*) as total from `met622`.`" + type + "." + days + "`");
					rs.next();
					if (rs.getInt("total") != 0) {
						System.out.println(type + ": records already exist.");
						continue;
					}

					String filename = days == "0" ? type : type + "." + days;
					BufferedReader reader = new BufferedReader(new FileReader(filename));
					String json;
					while ((json = reader.readLine()) != null) {
						String sql = "";
						org.bson.Document doc;
						String field1;
						String field2;
						String field3;
						String field4;
						String field5;
						try {
							switch (type) {
							case "SA/LightSensor":
								sql = "INSERT INTO `met622`.`SA/LightSensor" + "." + days + "` values('" + json + "');";
								stmt.executeUpdate(sql);
								break;
							case "ActivFit":
								doc = org.bson.Document.parse(json);
								field1 = doc.getString("sensor_name");
								field2 = ((org.bson.Document) doc.get("timestamp")).getString("start_time");
								field3 = ((org.bson.Document) doc.get("timestamp")).getString("end_time");
								field4 = ((org.bson.Document) doc.get("sensor_data")).getString("activity");
								field5 = ((org.bson.Document) doc.get("sensor_data")).getInteger("duration").toString();
								sql = "INSERT INTO `met622`.`ActivFit" + "." + days + "` values('" + field1 + "','"
										+ field2 + "','" + field3 + "','" + field4 + "','" + field5 + "');";
								stmt.executeUpdate(sql);
								break;
							case "HeartRate":
								doc = org.bson.Document.parse(json);
								field1 = doc.getString("sensor_name");
								field2 = doc.getString("timestamp");
								field3 = ((org.bson.Document) doc.get("sensor_data")).getInteger("bpm").toString();
								sql = "INSERT INTO `met622`.`HeartRate" + "." + days + "` values('" + field1 + "','"
										+ field2 + "','" + field3 + "');";
								stmt.executeUpdate(sql);
								break;
							}

						} catch (SQLException e) {
							System.out.println("MySQL SQL ignored: " + sql);
						}
					}
					reader.close();
				}
				con.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			System.out.println("MySQL " + days + " days imported.");
		}

	}

	/**
	 * Measure performance for (i)Brute Force, (ii)Lucene, (iii)MongoDB and
	 * (iv)MySQL.
	 */
	public static void measure(Results resultBF, Results resultLC, Results resultMD, Results resultMS)
			throws Exception {
		measure(resultBF, Method.BRUTE_FORCE);
		measure(resultLC, Method.LUCENE_INDEX);
		measure(resultMD, Method.MONGODB);
		measure(resultMS, Method.MYSQL);
	}

	/**
	 * Measure the time elapsed from reading data to retrieving query.
	 */
	public static void measure(Results results, Method method) throws Exception {
		for (String type : SENSOR_TYPES) {
			System.out.println(method.toString() + " " + type + " Response time measuring in progress...");
			// Make a query string.
			String searchField;
			String searchWord;
			String searchBF;
			String searchLC;
			String r = "";
			switch (type) {
			case "SA/LightSensor":
				searchField = "data";
				searchWord = "light";
				searchBF = "light";
				searchLC = "light";
				break;
			case "ActivFit":
				searchField = "sensor_data.activity";
				searchWord = "running";
				searchBF = "running";
				searchLC = "running";
				break;
			case "HeartRate":
				searchField = "timestamp";
				searchWord = "Wed Dec 21";
				searchBF = "Wed Dec 21";
				searchLC = "Wed Dec 21";
				break;
			default:
				throw new IllegalArgumentException("Unexpected value: " + type);
			}
			for (String days : DAYS) {
				// All merged data is not required for performance test.
				if ("0".equals(days))
					continue;
				// Start time.
				Long start = System.nanoTime();

				for (Integer i = 0; i < SIMULATION_COUNT; i++) {
					switch (method) {
					case BRUTE_FORCE:
						r = HWUtil.queryByBruteForce(type, days, searchBF, Integer.MAX_VALUE);
						break;
					case LUCENE_INDEX:
						r = HWUtil.queryByLucene(type, days, searchLC, Integer.MAX_VALUE);
						break;
					case MONGODB:
						r = HWUtil.queryByMongoDB(type, days, searchField, searchWord, Integer.MAX_VALUE);
						break;
					case MYSQL:
						r = HWUtil.queryByMySQL(type, days, searchField, searchWord, Integer.MAX_VALUE);
						break;
					}
					// Check if the query got some result.
					if (i == 0 && "100".contentEquals(days) && r.length() < 10)
						throw new Exception(
								"The query got nothing:" + method.toString() + " " + type + " " + days + " " + i);
					// Show progress of performance test.
					if (i % 100 == 0)
						System.out.println(method.toString() + " " + type + " " + days + " " + i);
				}
				// End time.
				Long finish = System.nanoTime();
				Long timeElapsed = finish - start;
				Float average = (((float) timeElapsed / 1000000) / SIMULATION_COUNT);
				// Update results.
				System.out.println(method.toString() + " " + days + " " + average.toString());
				results.get(type).put(days, average);
			}
		}
	}

	/**
	 * Make a query by Brute Force.
	 */
	public static String queryByBruteForce(String type, String days, String querystr, Integer limit) throws Exception {
		// Merged files.
		String filename = days == "0" ? type : type + "." + days;
		BufferedReader r = new BufferedReader(new FileReader(filename));
		String data;
		StringBuilder sb = new StringBuilder();
		Integer count = limit;
		while ((data = r.readLine()) != null) {
			// Search the query string.
			if (StringUtils.contains(data, querystr)) {
				sb.append(data);
				count--;
				if (count == 0)
					break;
			}
		}
		r.close();
		return sb.toString();
	}

	/**
	 * Make a query to test Lucene.
	 */
	public static String queryByLucene(String type, String days, String querystr, Integer limit) {
		try {
			StandardAnalyzer analyzer = new StandardAnalyzer();
			String indexName = days == "0" ? type + ".idx" : type + "." + days + ".idx";
			Directory index = FSDirectory.open(Paths.get(indexName));
			// Bug!(?) Somehow I need to open the directory
			// otherwise searcher does not return a correct result.
			IndexReader reader = DirectoryReader.open(index);
			IndexSearcher searcher = new IndexSearcher(reader);

			// the "data" arg specifies the default field to use when no field is
			// explicitly specified
			// in the query.
			Query q = new QueryParser("data", analyzer).parse(querystr);
			TopDocs docs = searcher.search(q, limit);
			ScoreDoc[] hits = docs.scoreDocs;
			// display results
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < hits.length; ++i) {
				int docId = hits[i].doc;
				Document d = searcher.doc(docId);
				sb.append((i + 1) + ". " + d.get("data"));
			}
			// reader can only be closed when there
			// is no need to access the documents any more.
			reader.close();
			return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * Make a query by MongoDB.
	 */
	public static String queryByMongoDB(String type, String days, String field, String querystr, Integer limit) {
		// Initiate a connection
		if (mongoClient == null)
			mongoClient = new MongoClient();
		MongoDatabase dbObj = mongoClient.getDatabase("machidahiroaki");
		MongoCollection<org.bson.Document> col = dbObj.getCollection(type.toLowerCase() + "." + days);
		MongoCursor<org.bson.Document> it = col.find(new org.bson.Document(field,
				new org.bson.Document().append("$regex", "^(?)" + Pattern.quote(querystr)).append("$options", "i")))
				.iterator();
		StringBuilder sb = new StringBuilder();
		Integer count = limit;
		while (it.hasNext()) {
			sb.append(it.next().toJson());
			count--;
			if (count == 0)
				break;
		}
		return sb.toString();
	}

	/**
	 * Make a query by MySQL.
	 */
	public static String queryByMySQL(String type, String days, String field, String querystr, Integer limit) {
		StringBuilder sb = new StringBuilder();
		try {
			if (mySQLConnection == null || mySQLConnection.isClosed())
				mySQLConnection = DriverManager.getConnection(
						"jdbc:mysql://localhost:3306/met622?useTimezone=true&serverTimezone=UTC", "root", "root");
			Statement stmt = mySQLConnection.createStatement();
			String sql = "select * from `met622`.`" + type + "." + days + "` where `" + field + "` like '%" + querystr
					+ "%'";
			ResultSet rs = stmt.executeQuery(sql);
			Integer count = limit;
			ResultSetMetaData metadata = rs.getMetaData();
			Integer columnCount = metadata.getColumnCount();
			while (rs.next()) {
				String row = "";
				for (int i = 1; i <= columnCount; i++) {
					row += rs.getString(i) + ", ";
				}
				sb.append(row);
				count--;
				if (count == 0)
					break;
			}
//			mySQLConnection.close();
		} catch (SQLException ex) {
			System.out.println(ex.getMessage());
		}
		return sb.toString();
	}

	/**
	 * Make performance test charts
	 */
	public static void makeImage(Results resultBF, Results resultLC, Results resultMD, Results resultMS)
			throws IOException {
		// Prepare the data set
		for (String type : HWUtil.SENSOR_TYPES) {
			DefaultCategoryDataset data = new DefaultCategoryDataset();
			// Chart for Brute Force.
			for (Entry<String, Float> entry : resultBF.get(type).entrySet()) {
				data.addValue(entry.getValue(), "Brute Force", entry.getKey());
			}
			// Chart for Lucene.
			for (Entry<String, Float> entry : resultLC.get(type).entrySet()) {
				data.addValue(entry.getValue(), "Lucene", entry.getKey());
			}
			// Chart for MongoDB.
			for (Entry<String, Float> entry : resultMD.get(type).entrySet()) {
				data.addValue(entry.getValue(), "MongoDB", entry.getKey());
			}
			// Chart for MySQL.
			for (Entry<String, Float> entry : resultMS.get(type).entrySet()) {
				data.addValue(entry.getValue(), "MySQL", entry.getKey());
			}
			JFreeChart chart = ChartFactory.createLineChart(type, "Number of days", "Response time(msec)", data,
					PlotOrientation.VERTICAL, true, false, false);
			ChartUtilities.saveChartAsJPEG(new File("./src/main/resources/image/" + type + ".jpg"), chart, 270, 300);
		}
	}

	@PreDestroy
	public static void closeConnections() {
		System.out.println("Connections closing.");
		try {
			if (mySQLConnection != null && !mySQLConnection.isClosed())
				mySQLConnection.close();
		} catch (SQLException e2) {
			e2.printStackTrace();
		}
		mongoClient.close();
		System.out.println("Connections closed.");

	}
}
