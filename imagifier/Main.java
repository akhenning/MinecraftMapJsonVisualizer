package imagifier;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;


/**
* I put everything into the main method (and thereby one file) because I'm used to Python and it is really short
 * 
 * @author @ahenning
 * @version 1/31/24
 */

public class Main {
	
	static int pixels_per_tile = 16;
	static int width = 128;
	static int height = 129;
	static int height_of_lowest_block = 64;
	static String count_direction = "from bottom";
	public static void main(String[] args) {
		System.out.println("Starting");
		File file = null;
		FileInputStream in = null;
		String raw_json = "error:nothing";
		try {
			file = new File("input json/input.json");
			in = new FileInputStream(file);

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
				int y = Integer.parseInt(y_str) + height_of_lowest_block;
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
		System.out.println("All blocks in schema: "+all_blocks.toString());
		
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
			for (int i = 0; i < width; i+=1) {
				for (int j = 0; j < height; j+=1) {
					if (data[i][j] != null){
						graphic2D.drawImage(map.get(data[i][j].name).texture, i*16, j*16, null);

						int diff = 0;
						if (count_direction == "from top" && j > 0) {
							diff = data[i][j].y-data[i][j-1].y;
						} else if (count_direction == "from bottom" && j < height-1) {
							diff = data[i][j].y-data[i][j+1].y;
						}
						if (diff == 1) {
							graphic2D.drawImage(map.get("plus").texture, i*16, j*16, null);
						} else if (diff > 1) {
							graphic2D.drawImage(map.get("double_plus").texture, i*16, j*16, null);
							System.out.println("Notice: Jump of more than one block at "+i+", "+j+". New Y: "+data[i][j].y);
						} else if (diff == -1) {
							graphic2D.drawImage(map.get("minus").texture, i*16, j*16, null);
						} else if (diff < -1) {
							graphic2D.drawImage(map.get("double_minus").texture, i*16, j*16, null);
							System.out.println("Notice: Drop of more than one block at "+i+", "+j+". New Y: "+data[i][j].y);
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

		System.out.println("Y values of top of image, in format [x,y]:");
		for (int i = 0; i < width; i+=1) {
			System.out.print("["+i+","+ data[i][1].y+"]");
			if (i != width-1) {
				System.out.print(",");
			}
		}
		System.out.println("\n");


		Map<Integer, Integer> heightMap = new HashMap<Integer, Integer>();

		System.out.println("Y values of bottom of image, in format [x,y]:");
		for (int i = 0; i < width; i+=1) {
			System.out.print("["+i+","+ data[i][height-1].y+"]");
			if (i != width-1) {
				System.out.print(",");
			}
			if (heightMap.containsKey(data[i][height-1].y)) {
				heightMap.put(data[i][height-1].y,heightMap.get(data[i][height-1].y) + 1);
			} else {
				heightMap.put(data[i][height-1].y,1);
			}
		}
		System.out.println("\n");

		System.out.println("Occurences of each height at bottom of image: \n"+heightMap.toString());

		
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
			try {
				texture = ImageIO.read(new File("images/" + block+"_top.png"));
			} catch (IOException e2) {
				System.out.println("ERROR: Could not open file " + block + ". Info: " + e.getLocalizedMessage());
			}
		}
	}
	public String toString() {
		return name;
	}
}