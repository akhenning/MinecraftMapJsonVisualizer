package imagifier;

import java.io.FileReader;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.imageio.ImageIO;


/**
 * Class that contains the main method for the program and creates the frame
 * containing the component.
 * 
 * @author @henning
 * @version 5/13/21
 */

public class Main {
	
	// Default values, will not normally be used
	boolean recalculate = true;
	boolean recalculate_avg = true;
	boolean recalculate_hue = true;
	boolean do_average = true;
	int precision = 3;
	static int pixels_per_tile = 16;
	static int width = 129;
	static int height = 129;
	boolean do_any_corrections = true;
	/**
	 * main method for the program which creates and configures the frame for the
	 * program
	 *
	 */
	public static void main(String[] args) {
		System.out.println("Starting");
		File file = null;
		FileInputStream in = null;
		String raw_json = "error:nothing";
		try {
			file = new File("input json/input.json");
			in = new FileInputStream(file);
			// out = new FileOutputStream("stages/1.txt");

			byte[] data = new byte[(int) file.length()];
			in.read(data);

			raw_json = new String(data, "UTF-8");
		} catch (Exception e) {
			System.out.println("Error when reading stage file");
		}
		if (in != null) {
			try {
				in.close();
			} catch (IOException e) {
				System.out.println("Error when closing stage file");
			}
		}

		int skipfirst = 0;

		ArrayList<String> all_blocks = new ArrayList<>();
		int minY = 999;
		int maxY = -64;
		
		BlockInfo[][] data = new BlockInfo[width][height];
		// Process level file
		String[] lines = raw_json.split("id\": \"minecraft:");
		for (String line : lines) {
			if (skipfirst == 0) {
				skipfirst += 1;
				continue;
			}
			//System.out.println(line);
			String name = line.substring(0 , line.indexOf("\""));
			//System.out.println(name);
			int xindex = line.indexOf("\"x\": ")+5;
			int yindex = line.indexOf("\"y\": ")+5;
			int zindex = line.indexOf("\"z\": ")+5;
			//System.out.println(line.substring(xindex,xindex+6));
			String x_str = line.substring(xindex, xindex+6).replaceAll("[^0-9]", "");
			String y_str = line.substring(yindex, yindex+6).replaceAll("[^0-9]", "");
			String z_str = line.substring(zindex, zindex+6).replaceAll("[^0-9]", "");
			//System.out.println(name+","+x_str+","+y_str+","+z_str);
			try {
				int x = Integer.parseInt(x_str);
				int y = Integer.parseInt(y_str);
				int z = Integer.parseInt(z_str);
				if (data[x][z] != null && data[x][z].y > y) {
					// Don't save if there is already something here and it is higher than the current block
					continue;
				}
				data[x][z] = new BlockInfo(name,y);
				if (minY > y) {
					minY = y;
				} else if (maxY < y) {
					maxY = y;
				}
				if (!all_blocks.contains(name)) {
					all_blocks.add(name);
				}
			} catch (Exception e) {
				System.out.println("Crap, something went wrong in interpreting the data. Error:"+e.toString());
				throw e;
			}			
		}

		System.out.println("Minimum Y value: "+minY);
		System.out.println("Maximum Y value: "+maxY);
		System.out.println("All blocks in it: "+all_blocks.toString());
		//for (int i = 0; i < all_blocks.size();i+=1) {
		//	if (all_blocks.get(i).contains("wool"))
		//}

		
		/*
		for (BlockInfo[] arr: data) {
			for (BlockInfo block: arr) {
				System.out.print(block+",");
			}
			System.out.println("\n");
		}
		*/

		Map<String, BlockImage> map = new HashMap<String, BlockImage>();
		for (String block : all_blocks) {
			map.put(block,new BlockImage(block));
		}
		map.put("double_minus",new BlockImage("double_minus"));
		map.put("minus",new BlockImage("minus"));
		map.put("double_plus",new BlockImage("double_plus"));
		map.put("plus",new BlockImage("plus"));
		
		// assumes square tiles right now
		BufferedImage image = new BufferedImage(pixels_per_tile * width,
				pixels_per_tile * height, BufferedImage.TYPE_INT_BGR);
		Graphics2D graphic2D = image.createGraphics();

		try {
			File output = new File("output/result.png");
			for (int i = 0; i < 129; i+=1) {
				for (int j = 0; j < 129; j+=1) {
					if (data[i][j] != null){
						graphic2D.drawImage(map.get(data[i][j].name).texture, i*16, j*16, null);

						if (j > 0) {
							int diff = data[i][j].y-data[i][j-1].y;
							if (diff == 1) {
								graphic2D.drawImage(map.get("plus").texture, i*16, j*16, null);
							} else if (diff > 1) {
								graphic2D.drawImage(map.get("double_plus").texture, i*16, j*16, null);
							} else if (diff == -1) {
								graphic2D.drawImage(map.get("minus").texture, i*16, j*16, null);
							} else if (diff < -1) {
								graphic2D.drawImage(map.get("double_minus").texture, i*16, j*16, null);
							}
						}
					} else {
						System.out.println("Nothing at "+i+", "+j+" for some reason");
					}
				}
			}
			ImageIO.write(image, "png", output);
		} catch (IOException log) {
			System.out.println(log);
		}
		System.out.println("Finished building output image.");
		/*
		try {
            
			// todo
			// This is for matching target to thingies?
			for (int i = 0; i < in.getWidth() - (pixels_per_tile / 2); i += pixels_per_tile) {
				for (int j = 0; j < in.getHeight() - (pixels_per_tile / 2); j += pixels_per_tile) {
					color = in.getRGB(i + (pixels_per_tile / 2), j + (pixels_per_tile / 2));
					rgb[2] = color & 0xff;
					rgb[1] = (color & 0xff00) >> 8;
					rgb[0] = (color & 0xff0000) >> 16;
					int a = ((color & 0xff000000) >> 24);
					if (a != -1 && a < 10) {
						continue;
					}
					String result = "";
					if (do_average) {
						result = closest(rgb);
						//System.out.println("In target file, for RGB " + rgb[0] + " " + rgb[1] + " " + rgb[2] + " " + a
						//		+ ", closest match is " + result);
					} else {
						Color.RGBtoHSB(rgb[0],rgb[1],rgb[2], hue);
						result = closest(hue);
						System.out.println("In target file, for hue " + hue[0] + " " + hue[1] + " " + hue[2] + " " + a
								+ ", closest match is " + result);
					}
					if (result == "") {
						System.out.println("WARNING: No good match (usually no matching dominant color).");
						continue;
					}
					BufferedImage tile = null;
					try {
						tile = ImageIO.read(new File("images/" + result));
					} catch (IOException e) {
						System.out
								.println("ERROR: Could not open file " + result + ". Info: " + e.getLocalizedMessage());
						return -1;
					}
					//System.out.println("Writing to image file, at location "+(int)((float)i * ((float)width / (float)pixels_per_tile))+", "+( j * (height / pixels_per_tile)));
					//System.out.println(i+" "+j+" "+width+" "+height+" "+pixels_per_tile);
				}
			}
			System.out.println("Writing to image.");
			
			ImageIO.write(image, "png", output);
		} catch (IOException log) {
			System.out.println(log);
		}
		System.out.println("Finished building output image.");
		return 1;*/
      
	}

}

class BlockInfo {
	public String name;
	public int y;
	public BlockInfo (String block, int y) {
		name = block;
		this.y = y;
	}
	public String toString() {
		return name+" at "+y;
	}
}


class BlockImage {
	public String name;
	public BufferedImage texture;
	public BlockImage (String block) {
		name = block;
		try {
			texture = ImageIO.read(new File("images/" + block+".png"));
		} catch (IOException e) {
			System.out.println("ERROR: Could not open file " + block + ". Info: " + e.getLocalizedMessage());
		}
	}
	public String toString() {
		return name;
	}
}