package com.github.rackyman.HHRipper;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;  
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;  
import org.jsoup.select.Elements;

class HHRipper{
	final static String version ="1.0";
	static String[] quality = {"1080","720","480","360","240"};
	static boolean download = false;
	static boolean noSpaces = false;
	static String overwrite = "prompt";
	static String downloadLocation = "downloads/";

	static File settings = new File("app.properties");
	static File list = new File("list.txt");
	static File downloads = new File("downloads.txt");

	public static void main(String[] a){
		try{
			BufferedReader rc = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("HENTAI HAVEN RIPPER  - v"+version+"\n");
			getSettings();
			if(!download)
				System.out.println("DOWNLOADS ARE TURNED OFF. Download links will be saved in "+downloads.getName()+"\n");
			System.out.print("\n1.Enter Link(s)\n2.Download from list.txt\n\nEnter your choice >>>>");
			switch(Integer.parseInt(rc.readLine())) {
			case 1:
				System.out.print("\nEnter Episode/Series Link [ Use , for multiple input] >>>> ");
				String links[] = rc.readLine().split(",");
				for(String link : links)
					extract(link);
				break;
			case 2:
				if(list.exists()) {
					BufferedReader br = new BufferedReader(new FileReader(list));
					String n="";
					while((n=br.readLine())!=null)
						extract(n);
					br.close();
				}
				else {
					FileWriter fw = new FileWriter(list);
					fw.flush();
					fw.close();
					System.out.println("List is empty. Please add some links and try again.");
				}
				break;
			default :
				System.out.println("INAVLID CHOICE PLEASE TRY AGAIN !!!!");
			}
		}catch(Exception e){
			System.out.println("Error at Main >>>>"+e);
		}
	}

	public static void extract(String link){
		try{
			System.out.println("\nLINK -> "+link);
			if(link.contains("hentaihaven")){
				if(link.contains("series"))
					extract_series(link.trim());
				else if(link.contains("episode"))
					extract_episode(link.trim());
				else
					System.out.println("Not a valid Link. ONLY SERIES & EPISODES ARE SUPPORTED FOR NOW !!!!");
			}
			else
				System.out.println("Not a HentaiHaven Link !!!!");
		}catch(Exception e){
			System.out.println("Error at extract >>>> "+e);
		}
	}
	
	public static void extract_series(String link){
		try{
			Document doc = Jsoup.connect(link).get();
			String title = doc.title().contains("|") ? doc.title().split("\\|")[0].trim() : doc.title().trim();
			System.out.println("Series -> "+title+"\n");
			//System.out.println("Fetching episodes...\n");
			Elements episodes = doc.select("a.brick-title");
			for(Element episode : episodes)
				//System.out.println(episode.text()+" >>>>>>> "+episode.attr("href"));
				extract_episode(episode.attr("href"));
		}catch(Exception e){
			System.out.println("Error at extract_series >>>> "+e);
		}
	}

	public static void extract_episode(String link){
		try{
			Document doc = Jsoup.connect(link).get();
			String title = doc.title().contains("|") ? doc.title().split("\\|")[0].trim() : doc.title().trim();
			System.out.println("Episode -> "+title);
			//System.out.println(doc.toString());
			Elements links = doc.select("a.btn[href*='hh.cx']");
			String dl_link = null;
			String dl_quality = null;
			mloop:
				for(Element l : links)
					if(l.attr("href").contains("hh.cx")){
						dl_quality = l.text();
						for(String q:quality)
							if(dl_quality.contains(q)) {
								dl_link = l.attr("href");
								break mloop;
							}
					}
			if(dl_link == null)
				System.out.println("Can't download. Specified Quality is Absent.");
			else {
				System.out.println(dl_quality+" -> "+dl_link);

				if(download){
					File video = new File(downloadLocation+title+".mp4");
					if(video.exists()) {
						if(overwrite.equals("skip"))
							System.out.println("FILE ALREADY DOWNLOADED. SKIPPED");
						else if(overwrite.equals("force")) {
							System.out.println("REDOWNLOADING FILE");
							copyURLToFile(new URL(dl_link), video);
						}
						else if(overwrite.equals("prompt")) {
							BufferedReader rc = new BufferedReader(new InputStreamReader(System.in));
							System.out.print("File is already downloaded. REDOWNLOAD (Y/N) ?");
							if(rc.readLine().toLowerCase().trim().equals("y")) {
								System.out.println("REDOWNLOADING FILE");
								copyURLToFile(new URL(dl_link), video);
							}
							else
								System.out.println("DOWNLOAD SKIPPED.");
						}
					}
					else
						copyURLToFile(new URL(dl_link), video);
				}
				else {
					BufferedWriter fw = new BufferedWriter(new FileWriter(downloads,true));
					fw.write(dl_link);
					fw.newLine();
					fw.flush();
					fw.close();
				}

			}
		}catch(Exception e){
			System.out.println("Error at extract_episode >>>> "+e);
		}
	}
	public static void copyURLToFile(final URL url, final File destination) throws IOException {
		Long start=0l;
		Long end=0l;
		final int EOF = -1;
		final int DEFAULT_BUFFER_SIZE = 1024 * 4;

		//System.out.println(destination.getAbsolutePath());
		final InputStream source=url.openStream();
		try {
			start=System.nanoTime();
			URLConnection urlConnection = url.openConnection();
			urlConnection.connect();
			int fileSize = urlConnection.getContentLength();
			System.out.println("File Size : "
					+(fileSize/1048576)
					+" MB");

			final FileOutputStream output = FileUtils.openOutputStream(destination);
			try {
				final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
				long count = 0;
				int n = 0;
				while (EOF != (n = source.read(buffer))) {
					output.write(buffer, 0, n);
					count += n;
					System.out.print("Downloading ["
							+count*100/fileSize
							+"%]\r");
				}
				System.out.println("Downloaded  ["
						+count*100/fileSize
						+"%]\r");

				output.close(); // don't swallow close Exception if copy completes normally
				end=System.nanoTime();
				System.out.println("Total time taken = " + TimeUnit.SECONDS.convert((end-start), TimeUnit.NANOSECONDS)+"s");
			} 
			catch(Exception e1){
				System.out.println(e1.getMessage());

			}
			finally {
				IOUtils.closeQuietly(output);
			}

		}
		catch(Exception e){
			System.out.println(e.getMessage());
		}
		finally {
			IOUtils.closeQuietly(source);
		}
	}
	public static void getSettings(){
		try {
			if(settings.exists()) {
				Properties prop = new Properties();
				prop.load(new FileReader(settings));

				download         = prop.getProperty("download").trim().matches("false|true") ? Boolean.parseBoolean(prop.getProperty("download").trim()) : download ;
				noSpaces         = prop.getProperty("noSpaces").trim().matches("false|true") ? Boolean.parseBoolean(prop.getProperty("noSpaces").trim()) : noSpaces ;
				overwrite        = prop.getProperty("overwrite").trim().matches("skip|force|prompt") ? prop.getProperty("overwrite").trim() : overwrite ;
				downloadLocation = prop.getProperty("downloadLocation").trim() !=null ? prop.getProperty("downloadLocation").trim() : downloadLocation ;
				quality          = prop.getProperty("quality").trim() !=null ? prop.getProperty("quality").trim().split(",") : quality ;

				if(downloadLocation.charAt(downloadLocation.length()) != '/');
					downloadLocation+="/";

				System.out.println("Successfully loaded Settings");
			}
			else
				createSettings();
		}catch(Exception e) {
			System.out.println("Error at getSettings "+e);
		}

	}
	public static void createSettings()throws IOException {
		System.out.println("Settings file not found. Creating new Settings File.");
		BufferedWriter bw = new BufferedWriter(new FileWriter(settings));
		bw.write("################################### HHRipper SETTINGS ###################################\r\n" + 
				"# HENTAI HAVEN RIPPER - Fap Them All !\r\n" + 
				"# Author - rackyman\r\n" + 
				"# Version - 1.0\r\n" + 
				"\r\n" + 
				"\r\n" + 
				"# If you mess up here just delete the file, and a new file with default settings will be created\r\n" + 
				"# Only use the supported values given below\r\n" + 
				"# For invalid values, deafault values will be used\r\n" + 
				"\r\n" + 
				"\r\n" + 
				"\r\n" + 
				"\r\n" + 
				"# Download videos in the most preferred quality\r\n" + 
				"# VALUES-1080,720,480,360,240\r\n" + 
				"# Use , for MULTIPLE VALUES\r\n" + 
				"# REMEMBER The prefernce is in descending order\r\n" + 
				"quality=1080,720,480,360,240\r\n" + 
				"\r\n" + 
				"# Show only title and download URL/Link\r\n" + 
				"# VALUES-true,false 	[DEFAULT-false]\r\n" + 
				"	#true  - Videos will be downloaded by the app\r\n" + 
				"	#false - Videos won't be downloaded, instead links will be STORED in \"downloads.txt\" file\r\n" + 
				"download=false\r\n" + 
				"\r\n" + 
				"# Replace SPACEs in name with UNDERSCORE\r\n" + 
				"# VALUES-true,false 	[DEFAULT-false]\r\n" + 
				"noSpaces=false\r\n" + 
				"\r\n" + 
				"# What to do when a episode is ALREADY Downloaded\r\n" + 
				"# VALUES-force,skip,prompt 		[DEFAULT-prompt]\r\n" + 
				"	#force - Force download video\r\n" + 
				"	#skip- Skips the download\r\n" + 
				"	#prompt - Ask for user input\r\n" + 
				"overwrite=prompt\r\n" + 
				"\r\n" + 
				"# Folder where the downloads will be placed [DEFAULT- downloads/]\r\n" + 
				"	#PASTE CUSTOM FOLDER PATH TO DOWNLOAD THERE\r\n" + 
				"		#eg- 'downloadLocation=D:/Downloads/fap' [WITHOUT THE 's]\r\n" + 
				"downloadLocation=downloads/");
		bw.flush();
		bw.close();
		getSettings();
	}
}
