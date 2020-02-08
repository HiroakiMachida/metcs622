package edu.bu.metcs662.machidahiroaki_final_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Boot Application.
 *
 */
@SpringBootApplication
public class App {

	/**
	 * Constructor. Processes before starting web service.
	 * 
	 * @throws InterruptedException
	 */
	public App() throws InterruptedException {
		super();
		try {
			System.out.println("==================================");
			System.out.println("   1. Get Data                    ");
			System.out.println("==================================");
			HWUtil.getData();

			System.out.println("==================================");
			System.out.println("   2. Merge Data                  ");
			System.out.println("==================================");
			HWUtil.merge();

			System.out.println("==================================");
			System.out.println("   3. Create Indices              ");
			System.out.println("==================================");
			HWUtil.createIndices();

			System.out.println("==================================");
			System.out.println("   4. Import to Database          ");
			System.out.println("==================================");
			HWUtil.importDataMongoDB();
			HWUtil.importDataMySQL();

			System.out.println("==================================");
			System.out.println("   5. Conduct Performance Test    ");
			System.out.println("==================================");
			Results resultBF = new Results();
			Results resultLC = new Results();
			Results resultMD = new Results();
			Results resultMS = new Results();
			HWUtil.measure(resultBF, resultLC, resultMD, resultMS);

			System.out.println("==================================");
			System.out.println("   6. Generate charts             ");
			System.out.println("==================================");
			HWUtil.makeImage(resultBF, resultLC, resultMD, resultMS);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Main function.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}
}
