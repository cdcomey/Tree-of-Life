import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;

import java.awt.Graphics;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.util.Stack;
import java.util.ArrayList;

// the Screen class is the bulk of the program
// it builds the tree, draws it, and handles node addition, editing, and saving
public class Screen extends JPanel implements KeyListener, MouseListener, ActionListener{
	private final int screenWidth = 1600;
	private final int screenHeight = 900;
	private final int nodeWidth = 100;	// how wide each node's image is. image height is scaled from this number
	private int maxHeight, leaves;
	private final int upperBound = 50;	// upper, left, right, and lower bounds delimit the farthest nodes can be drawn, providing a buffer between them and the screen edge
	private final int leftBound = upperBound;
	private final int lowerBound = 750;
	private final int rightBound = 1150;
	private final int displayLeftBound = rightBound + leftBound;	// the border between the tree and the display
	private final int buffer = 20;	// used for several things, like spacing between jcomponents and between node images
	private boolean editMode = false;	// if true, allows adding a new node or editing an old one
	private final String dummyString = Taxon.dummyText;	// used when the user did not fill in a field
	private Taxon root;	// root is the root of the subtree currently drawn, not necessarily of the whole tree
	private Taxon selectedTaxon;	// this taxon will be drawn in the display, and may be edited
	private Font font, bigFont;
	private FontMetrics fm, bfm;	// used for determining drawn string dimensions for spacing purposes
	
	// various jcomponents used for editing and adding nodes
	private JTextField nameField, parentField, commonNameField, imageField;
	private JComboBox rankComboBox;
	private JCheckBox extinctBox;
	private JTextArea traitsTextArea;
	private JScrollPane traitsScrollPane;
	private JButton addTaxonButton, editTaxonButton;
	
	@SuppressWarnings("unchecked")
	public Screen(){
		setLayout(null);
		setFocusable(true);
		
		addKeyListener(this);
		addMouseListener(this);
		
		font = new Font("Times New Roman", Font.PLAIN, 14);
		bigFont = new Font("Time New Roman", Font.BOLD, 24);
		
		// this is used so that the font metrics can be defined without needing a Graphics variable
		// used for initializing variables in the constructor, where there is no Graphics instance
		// canvas is not used outside of this one instance
		Canvas c = new Canvas();
		fm = c.getFontMetrics(font);
		bfm = c.getFontMetrics(bigFont);
		
		// the jcomponent bounds are mostly based off of other jcomponent bounds
		// this is because they are meant to be together, so if you want to change where the block is, necessary changes are minimal
		nameField = new JTextField();
		nameField.setBounds(displayLeftBound + buffer, screenHeight/3 + buffer, (screenWidth - displayLeftBound)/2 - 2*buffer, 30);
		add(nameField);
		
		commonNameField = new JTextField();
		commonNameField.setBounds(screenWidth - nameField.getWidth() - buffer, nameField.getY(), nameField.getWidth(), nameField.getHeight());
		add(commonNameField);
		
		String[] majorRanks = {"Unranked", "Domain", "Kingdom", "Phylum", "Class", "Order", "Family", "Genus", "Species"};
		rankComboBox = new JComboBox(majorRanks);
		rankComboBox.setBounds(nameField.getX(), nameField.getY() + 3*buffer, findComboBoxWidth(majorRanks), nameField.getHeight());
		add(rankComboBox);
		
		extinctBox = new JCheckBox("Extinct?");
		extinctBox.setBounds(screenWidth - (fm.stringWidth("Extinct?") + 30) - buffer, rankComboBox.getY(), fm.stringWidth("Extinct?") + 30, rankComboBox.getHeight());
		add(extinctBox);
		
		imageField = new JTextField();
		imageField.setBounds(nameField.getX(), rankComboBox.getY() + 3*buffer, screenWidth - displayLeftBound - 2*buffer, nameField.getHeight());
		add(imageField);
		imageField.addActionListener(this);
		
		parentField = new JTextField();
		parentField.setBounds(nameField.getX(), screenHeight - nameField.getHeight() - buffer, nameField.getWidth(), nameField.getHeight());
		add(parentField);
		
		addTaxonButton = new JButton("Add Taxon");
		addTaxonButton.setBounds(commonNameField.getX(), parentField.getY(), commonNameField.getWidth(), parentField.getHeight());
		add(addTaxonButton);
		addTaxonButton.addActionListener(this);
		
		editTaxonButton = new JButton("Edit Taxon");
		editTaxonButton.setBounds(displayLeftBound + (screenWidth - displayLeftBound - addTaxonButton.getWidth())/2, screenHeight - 60 - buffer, addTaxonButton.getWidth(), 60);
		add(editTaxonButton);
		editTaxonButton.addActionListener(this);
		
		// you may have a lot to write for a taxon's traits, so a text area with scroll pane is used instead of a text field
		traitsTextArea = new JTextArea();
		traitsTextArea.setLineWrap(true);
		traitsTextArea.setWrapStyleWord(true);
		add(traitsTextArea);
		
		traitsScrollPane = new JScrollPane(traitsTextArea); 
        traitsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        traitsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		traitsScrollPane.setBounds(imageField.getX(), imageField.getY() + 3*buffer, imageField.getWidth(), parentField.getY() - 3*buffer - (imageField.getY() + 2*buffer));
		add(traitsScrollPane);
		
		// jcomponents are initially invisible because editMode is false
		updateComponentVisibility();
		
		// this block loads in the tree from a text file
		File treeFile = new File("tol.txt");
		 try (FileInputStream fis = new FileInputStream(treeFile);
                BufferedInputStream bis = new BufferedInputStream(fis)) {
		String s = new String(bis.readAllBytes());					// loads the file's contents into a string
		String[] taxons = s.split("end\n\n");									// each element in the array is the text form of a taxon
		for (int i = 0; i < taxons.length; i++){
			String[] info = taxons[i].split(";");								// each element is some information about the taxon, eg name, rank
			
			// this if block handles adding the root Taxon
			if (root == null && info[3].equals("null")){
				// Taxon constructor parameters are: name, parent, commonName, rank, traits, imageURL, extinct
				// and is stored as: name, commonName, rank, parent, traits, imageURL, extinct
				root = new Taxon(info[0], null, info[1], info[2], info[4], info[5], Boolean.parseBoolean(info[6]));	// root's parent is stored as null
			} else{	// this else block handles adding all other Taxons
				// most of this block is for finding the new taxon's parent
				// this is because parent is a Taxon, but must be saved as a string
				Stack<Taxon> stack = new Stack<Taxon>();	// the stack is used for a DFS of the tree
				Taxon parent = null;												// temp variable used to exit the DFS once the parent has been found
				stack.push(root);
				while (!stack.isEmpty()){
					Taxon activeTaxon = stack.pop();
					for (Taxon each : activeTaxon.children()){
						stack.push(each);
					}
				
					if (activeTaxon.name().equals(info[3])){		// info[3] is the taxon's parent's name
						parent = activeTaxon;
						break;
					}
				}
				
				if (parent == null)
					throw new NullPointerException("parent for " + info[0] + " was not found");
				Taxon node = new Taxon(info[0], parent, info[1], info[2], info[4], info[5], Boolean.parseBoolean(info[6]));
			}
		}
		} catch (FileNotFoundException e){
			System.err.println("FileNotFoundException in Screen() while building the tree");
		} catch (IOException e){
			System.err.println("IOException in Screen() while building the tree");
		}
		
		// this builds the initial subtree, rooted at the root of the full tree
		updateNodeCoords();
	}
	
	public Dimension getPreferredSize(){
		return new Dimension(screenWidth, screenHeight);
	}
	
	// handles drawing
	public void paintComponent(Graphics g){
		super.paintComponent(g);
		
		// gray background
		g.setColor(Color.gray);
		g.fillRect(0, 0, screenWidth, screenHeight);
		
		// draws line separating tree from display
		Graphics2D g2 = (Graphics2D)g;
		g2.setStroke(new BasicStroke(3));
		g.setColor(Color.black);
		g.drawRect(displayLeftBound, 0, 1, screenHeight);
		
		// jcomponents are also visible, but is handled elsewhere to reduce run time, since paintComponent is called often and unpredictably
		if (editMode){
			// strings describing the purpose of the component are drawn above them
			drawString(g, "Scientific Name", nameField);
			drawString(g, "Common Name", commonNameField);
			drawString(g, "Rank", rankComboBox);
			drawString(g, "Parent", parentField);
			drawString(g, "Image URL", imageField);
			drawString(g, "Traits", traitsScrollPane);
			
			// the selected taxon is drawn above the display, mainly so the user can see the image
			if (selectedTaxon != null){
				selectedTaxon.drawMe(g, displayLeftBound + (screenWidth - displayLeftBound)/2, nameField.getY()/2 - 2*fm.getHeight(), screenWidth-displayLeftBound - 2*buffer, nameField.getY() - 4*buffer, true);
			}
		} else if (!editMode && selectedTaxon != null){
			g.setFont(bigFont);
			selectedTaxon.drawMe(g, displayLeftBound + (screenWidth - displayLeftBound)/2, screenHeight/4, screenWidth-displayLeftBound - 2*buffer, screenHeight/2 - 2*buffer, true);
		}
		
		g2.setStroke(new BasicStroke(10));	// this stroke size is used for the lines between nodes
		
		g.setFont(font);
		drawNodes(g);										// draws the subtree
	}
	
	//draws string just above a given text field or other JComponent
	private void drawString(Graphics g, String text, JComponent component){
		if (component.isVisible())
			g.drawString(text, component.getX() + component.getWidth()/2 - g.getFontMetrics().stringWidth(text)/2, component.getY()-5);
	}
	
	//draws string centered on x
	private void drawString(Graphics g, String line, int x, int y){
		g.drawString(line, x  - fm.stringWidth(line) / 2, y);
	}
	
	// this was used for debugging
	/* private String printCoords(int x, int y){
		return "(" + x + ", " + y + ")";
	} */
	
	// finds how many 'levels' of nodes should be drawn
	private int findMaxHeight(Taxon root, int lowerBound, int upperBound, int nodeSize, int nodeGap){
		int maxHeightLocal = 0;
		// if the root is of a major rank, the limit should be the farthest away a descendant of the next major rank is
		// maxHeightLocal keeps track of this value throughout the DFS
		if (root.majorRank()){
			Stack<Taxon> stack = new Stack<Taxon>();
			stack.push(root);
			while (!stack.isEmpty()){
				Taxon activeTaxon = stack.pop();
				
				if (activeTaxon.majorRank() && activeTaxon.numRank() > root.numRank()){
					maxHeightLocal = Math.max(maxHeightLocal, root.height() - activeTaxon.height());
				} else {	// children of activeTaxon should not be explored if activeTaxon is a major rank
					for (Taxon each : activeTaxon.children())
						stack.push(each);
				}
			}
		} 
		// if the root is not of a major rank, the limit should be the farthest away a descendant of any major rank is
		else {
			Stack<Taxon> stack = new Stack<Taxon>();
			stack.push(root);
			while (!stack.isEmpty()){
				Taxon activeTaxon = stack.pop();
				
				if (activeTaxon.majorRank()){
					maxHeightLocal = Math.max(maxHeightLocal, root.height() - activeTaxon.height());
				} else {
					for (Taxon each : activeTaxon.children())
						stack.push(each);
				}
			}
		}
		
		// the maxHeight may cause too many nodes to be drawn so that they overlap horizontally
		// so decrease it until this is no longer the case
		while (root.width(maxHeightLocal, 0) * (nodeWidth + buffer) >= displayLeftBound){
			maxHeightLocal--;
		}
		
		// if the node has no children, it will return 0, but the root itself should always be drawn
		// so the height should always be at least 1
		return Math.max(1, maxHeightLocal);
	}
	
	// this finds the y-coord of a node to be drawn
	private int findNodeY(Taxon taxon, int ancestors, int lowerBound, int upperBound, int maxHeight){
		// if a child can't be fit above this node, place this node at upperBound to ensure they are all aligned at the end
		if ((lowerBound - (ancestors+1) * ((lowerBound - upperBound) / maxHeight)) < upperBound)
			return upperBound;
		// if a node is at the next major rank, draw it at upperBound
		if (taxon.majorRank() && ancestors > 0)
			return upperBound;
		// if a node has no children, draw it at upperBound
		if (taxon.children().size() == 0)
			return upperBound;
		// otherwise, draw it on a 'level' based on how many generations from the root it is
		return lowerBound - ancestors * ((lowerBound - upperBound) / maxHeight);
	}
	
	// this finds the x-coord of a node to be drawn
	private int findNodeX(Taxon taxon, int leftBound, int rightBound, boolean isLeaf){
		// nodeSpacing is how far apart each node should be drawn
		// it uses the number of descendants to be drawn at upperBound
		int nodeSpacing = (rightBound - leftBound) / Math.max(1, (root.width(maxHeight, 0)-1));
		if (isLeaf)	// if the node is a leaf (drawn at upperBound), it should be drawn evenly spaced next to the already-drawn nodes
			return leftBound + leaves * nodeSpacing;
		else {			// otherwise, the node should be drawn in between its leftmost and rightmost descendants
			int leftX = leftBound + leaves * nodeSpacing;
			int rightX = leftX + (taxon.width(Math.max(maxHeight - (root.height() - taxon.height()), 1), 0) - 1) * nodeSpacing;
			return (leftX+rightX)/2;
		}
	}
	
	// rescursively sets coords for a node and its children
	// Taxon taxon is the taxon to be drawn (should not be changed)
	// int ancestors is the number of taxon's ancestors already drawn. starts at 0 for the root and increments every recursive call
	private void determineNodeCoords(Taxon taxon, int ancestors){
		int y = findNodeY(taxon, ancestors, lowerBound, upperBound, maxHeight);
		int x = findNodeX(taxon, leftBound, rightBound, y == upperBound);
		taxon.setCoords(x - nodeWidth/2, y - nodeWidth/2, nodeWidth, nodeWidth);
		
		if (y > upperBound){	// if the node is at upperBound, its children won't be drawn, so no need to update coords
			if (taxon.leftChild() != null)
				determineNodeCoords(taxon.leftChild(), ancestors+1);			
			if (taxon.rightChild() != null)
				determineNodeCoords(taxon.rightChild(), ancestors+1);
		} else if (y == upperBound)	// leaves is used for finding node x
			leaves++;
	}
	
	// recursively draws nodes on the subtree and the lines between them
	// node coords are already saved in the Taxons, so only Graphics need be passed in
	private void drawNodes(Graphics g){
		Stack<Taxon> stack = new Stack<Taxon>();
		stack.push(root);
		while (!stack.isEmpty()){
			Taxon activeTaxon = stack.pop();
			Rectangle coords = activeTaxon.getCoords();
			for (Taxon each : activeTaxon.children()){
				if (each.getCoords() != null){
					stack.push(each);
					Rectangle coords2 = each.getCoords();
					g.setColor(Color.black);
					
					if (!each.extinct())
						g.drawLine(coords.x() + coords.width()/2, coords.y() + coords.height()/2, coords2.x() + coords2.width()/2, coords2.y() + coords2.height()/2);
					else
						drawDashedLine(g, coords.x() + coords.width()/2, coords.y() + coords.height()/2, coords2.x() + coords2.width()/2, coords2.y() + coords2.height()/2);
				}
			}
			
			// the taxon's name is red if at a major rank, and orange if not
			g.setColor(activeTaxon.majorRank() ? Color.red : Color.orange);
			activeTaxon.drawMe(g, coords.x(), coords.y(), coords.width(), coords.height(), false);
		}
	}
	
	// these are necessary to define for implementing MouseListener, but are not used
	public void mouseExited(MouseEvent e){}
	public void mouseEntered(MouseEvent e){}
	public void mouseReleased(MouseEvent e){}
	public void mousePressed(MouseEvent e){}
	
	// selects taxon if one is clicked on, and deselects taxon if a blank space is clicked
	public void mouseClicked(MouseEvent e){
		int mouseX = e.getX();
		int mouseY = e.getY();
		// System.out.println(printCoords(mouseX, mouseY));
		boolean foundNode = false;
		
		// iterate through the subtree until the clicked-on node has been found
		Stack<Taxon> stack = new Stack<Taxon>();
		stack.push(root);
		while (!stack.isEmpty()){
			Taxon activeTaxon = stack.pop();
			Rectangle coords = activeTaxon.getCoords();
			
			if (mouseX >= coords.x() && mouseX <= coords.x2() && mouseY >= coords.y() && mouseY <= coords.y2()){	// if the node is clicked on
				selectedTaxon = activeTaxon;
				foundNode = true;
				updateComponentVisibility();
				updateComponentText();
				traitsTextArea.setText(selectedTaxon.traits());
				break;
			}
			
			for (Taxon each : activeTaxon.children()){
				if (each.getCoords() != null){	// this checks whether its children have been drawn. if it hasn't, don't check them
					stack.push(each);
				}
			}
		}
		
		// if no node was clicked on, deselect the current node and hide the edit jcomponents
		if (!foundNode){
			selectedTaxon = null;
			updateComponentVisibility();
		}
		
		repaint();
	}
	
	public void actionPerformed(ActionEvent e){
		if (e.getSource() == addTaxonButton){
			editMode = false;
			addNewTaxon();
			updateComponentVisibility();
			updateComponentText();
		} else if (e.getSource() == editTaxonButton){
			editMode = true;
			updateComponentVisibility();
			updateComponentText();
			parentField.setEditable(false);
		}
		
		repaint();
	}
	
	public void keyTyped(KeyEvent e){}
	public void keyReleased(KeyEvent e){}
	
	public void keyPressed(KeyEvent e){
		// System.out.println(e.getKeyCode());
		// changes the root to the next major-rank ancestor
		if (e.getKeyCode() == 8){ //backspace
			if (selectedTaxon != null){
				do {
					selectedTaxon = selectedTaxon.parent();
				} while (!selectedTaxon.majorRank() && selectedTaxon.parent() != null);
				root = selectedTaxon;
				updateNodeCoords();
			}
		} 
		// deselects node
		else if (e.getKeyCode() == 27){ //escape
			selectedTaxon = null;
		} 
		// roots the subtree at selectedTaxon
		else if (e.getKeyCode() == 32){ //space
			if (selectedTaxon != null && selectedTaxon.children().size() > 1){
				root = selectedTaxon;
				updateNodeCoords();
			}
		} 
		// toggles editMode
		else if (e.getKeyCode() == 112){ //F1
			editMode = !editMode;
			updateComponentVisibility();
			updateComponentText();
		}
		
		repaint();
	}
	
	private void updateNodeCoords(){
		Stack<Taxon> stack = new Stack<Taxon>();
		stack.push(root);
		while (!stack.isEmpty()){
			Taxon activeTaxon = stack.pop();
			activeTaxon.resetCoords();
			for (Taxon each : activeTaxon.children())
				stack.push(each);
			
		}
		maxHeight = findMaxHeight(root, lowerBound, upperBound, nodeWidth, 100);
		leaves = 0;
		determineNodeCoords(root, 0);
	}
	
	private void updateComponentVisibility(){
		/* components should be visible if editMode is on
		*	the exception is editTaxonButton, which activates editMode
		*	traitsScrollPane should also display if a taxon is selected
		*	finally, parentField should not be editable if a taxon were selected, as otherwise the tree could be messed up
		*/
		nameField.setVisible(editMode);
		commonNameField.setVisible(editMode);
		rankComboBox.setVisible(editMode);
		extinctBox.setVisible(editMode);
		parentField.setVisible(editMode);
		parentField.setEditable(editMode && selectedTaxon == null);
		imageField.setVisible(editMode);
		addTaxonButton.setVisible(editMode);
		traitsScrollPane.setVisible(editMode || selectedTaxon != null);
		traitsTextArea.setEditable(editMode);
		editTaxonButton.setVisible(!editMode && selectedTaxon != null);
	}
	
	// similar to updateComponentVisibility
	// sets field text to selectedTaxon info, or nothing if not selected
	private void updateComponentText(){
		if (selectedTaxon != null){
			nameField.setText(selectedTaxon.name());
			commonNameField.setText(selectedTaxon.commonName());
			rankComboBox.setSelectedItem(selectedTaxon.rank());
			parentField.setText((selectedTaxon.parent() == null) ? "null" : selectedTaxon.parent().name());
			imageField.setText(selectedTaxon.imageURL());
			traitsTextArea.setText(selectedTaxon.traits());
			extinctBox.setSelected(selectedTaxon.extinct());
		} else {
			nameField.setText("");
			commonNameField.setText("");
			rankComboBox.setSelectedItem(0);
			parentField.setText("");
			imageField.setText("");
			traitsTextArea.setText("");
			extinctBox.setSelected(false);
		}
	}
	
	// handles adding and editing taxons, and saving
	private void addNewTaxon(){
		// get info from jcomponents
		String name = (nameField.getText().length() > 0) ? nameField.getText() : dummyString;
		String commonName = (commonNameField.getText().length() > 0) ? commonNameField.getText() : dummyString;
		String rank = (String)rankComboBox.getSelectedItem();
		boolean extinct = extinctBox.isSelected();
		String traits = (traitsTextArea.getText().length() > 0) ? traitsTextArea.getText() : dummyString;
		String imageURL = (imageField.getText().length() > 0) ? imageField.getText() : dummyString;
		String parentString = (parentField.getText().length() > 0) ? parentField.getText() : dummyString;
		
		Taxon trueRoot = root;		// the root of the entire tree, not just the subtree drawn
		Stack<Taxon> stack = new Stack<Taxon>();
		
		if (parentString.equals("null")){	// if the node to be added is the true root
			if (selectedTaxon != null){
				ArrayList<Taxon> children = selectedTaxon.children();
				trueRoot = new Taxon(name, null, commonName, rank, traits, imageURL, extinct);
				selectedTaxon = trueRoot;
				for (Taxon each : children)	// since trueRoot is a new Taxon, its children must be readded
					trueRoot.addChild(each);
			} else {
				trueRoot = new Taxon(name, null, commonName, rank, traits, imageURL, extinct);
				selectedTaxon = trueRoot;
			}
		} else {
			while (trueRoot.parent() != null)	// loop finds the true root
				trueRoot = trueRoot.parent();
			stack.push(trueRoot);
			while (!stack.isEmpty()){
				Taxon activeTaxon = stack.pop();
				for (Taxon each : activeTaxon.children()){
					stack.push(each);
				}
				
				// this block searches through the full tree to find the parent specified by the new taxon
				if (activeTaxon.name().equals(parentString)){
					if (selectedTaxon != null){
						ArrayList<Taxon> children = selectedTaxon.children();
						Taxon taxon = new Taxon(name, activeTaxon, commonName, rank, traits, imageURL, extinct);
						selectedTaxon = taxon;
						if (taxon.equals(selectedTaxon)){
							for (Taxon each : children)
								taxon.addChild(each);
						}
					} else {
						Taxon taxon = new Taxon(name, activeTaxon, commonName, rank, traits, imageURL, extinct);
						selectedTaxon = taxon;
					}
					
					break;
				}
			}
		}
		
		// this section saves the full tree to a string
		// System.out.println("\nsaving...");
		String s = "";
		stack = new Stack<Taxon>();	// stack should be empty without this line, but it apparently isnt
		stack.push(trueRoot);
		while (!stack.isEmpty()){
			Taxon activeTaxon = stack.pop();
			// System.out.println(counter + " " + activeTaxon.name());
			for (Taxon each : activeTaxon.children()){
				// System.out.println("\t" + each.name());
				stack.push(each);
			}
			
			s += activeTaxon.toString() + "end\n\n";
		}
		
		// this block saves that string as a text file
		try(FileOutputStream fos = new FileOutputStream("tol.txt");
                BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            //convert string to byte array
            byte[] bytes = s.getBytes();
            //write byte array to file
            bos.write(bytes);
            bos.close();
            fos.close();
            // System.out.println("Data written to file successfully.");
        } catch (IOException e) {
			System.err.println("Exception occurred in save()");
            e.printStackTrace();
        }
		
		// other nodes may have to be readjusted
		traitsTextArea.setText(selectedTaxon.traits());
		updateNodeCoords();
	}
	
	// finds the min width of a jcombobox to comfortably fit the length of all the strings it displays
	private int findComboBoxWidth(String[] items){
		int max = 0;
		for (String each : items){
			max = Math.max(max, fm.stringWidth(each));
		}
		
		return max + 30; // the drop-down button is about 30 units long
	}
	
	// credit to stackoverflow
	// used to draw a line to an extinct taxon
	public void drawDashedLine(Graphics g, int x1, int y1, int x2, int y2){
		//creates a copy of the Graphics instance
		Graphics2D g2d = (Graphics2D) g.create();

		BasicStroke dashed = new BasicStroke(10, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
		g2d.setStroke(dashed);
		g2d.drawLine(x1, y1, x2, y2);

		//gets rid of the copy
		g2d.dispose();
    }
}