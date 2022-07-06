import java.awt.Graphics;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;

import java.io.IOException;

import java.net.URL;

import javax.imageio.ImageIO;

import java.util.ArrayList;

// class that contains information about each taxon
public class Taxon{
	private boolean majorRank;
	private Taxon parent, leftChild, rightChild;
	private String name; //eg chordata
	private String commonName;
	private String rank; //eg phylum
	private static final String[] majorRanks = {"LUCA", "Domain", "Kingdom", "Phylum", "Class", "Order", "Family", "Genus", "Species"};
	private int numRank; // eg 3
	
	private String traits;
	private String imageURL;
	private boolean extinct;
	public static final String dummyText = "???";
	
	private Rectangle coords;
	
	// this constructor was used for debugging purposes
	/* public Taxon(String name, Taxon parent){
		this.name = name;
		this.parent = parent;
		this.commonName = dummyText;
		this.rank = dummyText;
		this.traits = dummyText;
		this.imageURL = dummyText;
		this.extinct = false;
		
		if (this.parent != null){
			this.parent.addChild(this);
		}
		
		numRank = determineNumRank();
	} */
	
	public Taxon(String name, Taxon parent, String commonName, String rank, String traits, String imageURL, boolean extinct){
		this.name = name;
		this.parent = parent;
		this.commonName = commonName;
		this.rank = rank;
		this.traits = traits;
		this.imageURL = imageURL;
		this.extinct = extinct;
		
		if (this.parent != null){
			this.parent.addChild(this);
		}
		
		numRank = determineNumRank();
	}
	
	private int determineNumRank(){
		for (int i = 0; i < majorRanks.length; i++){
			if (rank.equals(majorRanks[i])){
				majorRank = true;
				return i+1;
			}
		}
		
		return 0;
	}
	
	public Taxon leftChild(){ return leftChild; }
	public Taxon rightChild(){ return rightChild; }
	
	public ArrayList<Taxon> children(){
		ArrayList<Taxon> list = new ArrayList<Taxon>();
		if (leftChild != null)
			list.add(leftChild);
		if (rightChild != null)
			list.add(rightChild);
		return list;
	}
	
	public boolean majorRank(){ return majorRank; }
	public int numRank(){ return numRank; }
	public String rank(){ return rank; }
	public String name(){ return name; }
	public String commonName(){ return commonName; }
	public String traits(){ return traits; }
	public String imageURL(){ return imageURL; }
	public Taxon parent(){ return parent; }
	public boolean extinct(){ return extinct; }
	
	// this method handles both adding new children and modifying old children
	public void addChild(Taxon child){
		// first half of conditional is for adding children, second half is for modifying old children
		if (leftChild == null || leftChild.equals(child))
			leftChild = child;
		else if (rightChild == null || rightChild.equals(child))
			rightChild = child;
		// this portion 'balances' the tree by ensuring the half with more children is on the right
		// this is to keep the tree looking neater
		// it is not perfect, since width() is used instead of width(height), but it is close enough, and it is less efficient to pass in maxHeight every time
		if (rightChild != null && rightChild.width() < leftChild.width()){
			Taxon temp = rightChild;
			rightChild = leftChild;
			leftChild = temp;
		}
	}
	
	// height() finds the number of nodes separating it from a node of the next taxonomic rank
	public int height(){
		if (leftChild == null && rightChild == null)
			return 0;
		if (leftChild == null)
			return rightChild.height() + 1;
		if (rightChild == null)
			return leftChild.height() + 1;
		if (leftChild.majorRank() && rightChild.majorRank())
			return 1;
		if (leftChild.majorRank())
			return rightChild.height() + 1;
		if (rightChild.majorRank())
			return leftChild.height() + 1;
		return Math.max(leftChild.height(), rightChild.height()) + 1;
	}
	
	// width() finds the number of leaf nodes it connects to
	// in this method, leaf nodes are defined as those with no children or those of a major rank
	// in practice, this method is never used because the entire tree cannot be printed at once
	public int width(){
		if (leftChild == null && rightChild == null)
			return 1;
		if (leftChild == null)
			return rightChild.width();
		if (rightChild == null)
			return leftChild.width();
		if (leftChild.majorRank() && rightChild.majorRank())
			return 2;
		if (leftChild.majorRank())
			return rightChild.width() + 1;
		if (rightChild.majorRank())
			return leftChild.width() + 1;
		return leftChild.width() + rightChild.width();
	}
	
	// width() finds the number of leaf nodes it connects to
	// in this method, leaf nodes are defined as those with no children or those of a major rank, up to a certain distance from the root
	public int width(int maxHeight, int counter){
		counter++;
		if (counter > maxHeight)
			return 1;
		if (leftChild == null && rightChild == null)
			return 1;
		if (leftChild == null)
			return rightChild.width(maxHeight, counter);
		if (rightChild == null)
			return leftChild.width(maxHeight, counter);
		if (leftChild.majorRank() && rightChild.majorRank())
			return 2;
		if (leftChild.majorRank())
			return rightChild.width(maxHeight, counter) + 1;
		if (rightChild.majorRank())
			return leftChild.width(maxHeight, counter) + 1;
		return leftChild.width(maxHeight, counter) + rightChild.width(maxHeight, counter);
	}
	
	public String toString(){
		String s = "";
		s += name + ";";
		s += commonName + ";" + rank + ";";
		s += (parent == null ? "null" : parent.name()) + ";";
		s += traits + ";" + imageURL + ";" + extinct + ";";
		
		return s;
	}
	
	// this is only for debugging purposes
	public String toStringReadable(){
		String s = "";
		s += "Name: " + name + ", aka " + commonName + "\n";
		s += "Rank: " + rank + "\n";
		s += (parent == null) ? ("Root") : ("Part of " + parent.name()) + "\n";
		s += "Traits: " + traits + "\n";
		s += "image URL: " + imageURL + "\n";
		s += "is " + (extinct ? "" : "not ") + "extinct\n";
		
		return s;
	}
	
	// (x,y) specifies top left corner of image if centered is false
	public void drawMe(Graphics g, int x, int y, int maxWidth, int maxHeight, boolean centered){
		int width = 0;
		int height = 0;
		BufferedImage image = null;
		try{
			URL link = new URL(imageURL);
			image = ImageIO.read(link);
			width = image.getWidth();
			height = image.getHeight();
			
			// this section resizes the image in proportion to its original size to fit within maxWidth and maxHeight
			if (width > maxWidth && height <= maxHeight){
				height = (int)((double)(maxWidth)/width * height);
				width = maxWidth;
			} else if (width <= maxWidth && height > maxHeight){
				width = (int)((double)(maxHeight)/height * width);
				height = maxHeight;
			} else if (width > maxWidth && height > maxHeight){
				if (height - maxHeight >= width - maxWidth){
					width = (int)((double)(maxHeight)/height * width);
					height = maxHeight;
				} else {
					height = (int)((double)(maxWidth)/width * height);
					width = maxWidth;
				}
			}
			
			// if the image is to be centered, set the coords to be the center
			if (centered){
				x -= width/2;
				y -= height/2;
			}
				
			g.drawImage(image, x, y, width, height, null);
		} catch (IOException e){
			// this part is intended to run when no image is saved for this taxon
			BufferedImage img = null;
			try{
				// placeholder image
				URL link = new URL("https://archives.bulbagarden.net/media/upload/archive/8/8e/20090709005535%21Spr_3r_000.png");
				img = ImageIO.read(link);
				width = Math.min(maxWidth, maxHeight); // this image is square, so resizing it is easier
				height = width;
				if (centered){
					x -= width/2;
					y -= height/2;
				}
				g.drawImage(img, x, y, width, height, null);
			} catch (IOException ex){
				System.err.println("IOException in Taxon.drawImage()");
				e.printStackTrace();
			}
		}
		
		// draws the text under the image
		drawString(g, name, x + width/2, y + height + g.getFontMetrics().getHeight(), width + 50);
		if (!commonName.equals(dummyText))
			drawString(g, "(" + commonName + ")", x + width/2, y + height + 2*g.getFontMetrics().getHeight(), width + 50);
	}
	
	// this method splits the taxon's name if it is too long
	private void drawString(Graphics g, String text, int x, int y, int maxWidth){
		FontMetrics fm = g.getFontMetrics();
		if (fm.stringWidth(text) > maxWidth && text.contains("+")){
			String substring1 = text.substring(0, text.indexOf("+")+1);
			String substring2 = text.substring(text.indexOf("+")+1, text.length());
			g.drawString(substring1, x - fm.stringWidth(substring1) / 2, y);
			g.drawString(substring2, x - fm.stringWidth(substring2) / 2, y + fm.getHeight());
		} else if (fm.stringWidth(text) > maxWidth){
			String lineString = "";
			int lines = 0;
			for (String line : text.split(" ")){
				if (fm.stringWidth(lineString + " " + line) > maxWidth && lineString.length() > 0){
					g.drawString(lineString, x - fm.stringWidth(lineString)/2, y + lines * fm.getHeight());
					lineString = line;
					lines++;
				} else
					lineString += " " + line;
			}
			
			g.drawString(lineString, x - fm.stringWidth(lineString)/2, y + lines * fm.getHeight());
		} else {
			g.drawString(text, x - fm.stringWidth(text) / 2, y);
		}
	}
	
	public void setCoords(int x, int y, int width, int height){
		coords = new Rectangle(x, y, width, height);
	}
	
	public void resetCoords(){ coords = null; }
	
	public Rectangle getCoords(){ return coords; }
	
	public boolean equals(Taxon t){
		return name.equals(t.name()) && ((parent == null && t.parent() == null) || parent.name().equals(t.parent().name()));
	}
}