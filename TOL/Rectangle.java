import java.awt.Graphics;

public class Rectangle{
	
	private final int x, x2, y, y2, width, height;
	
	public Rectangle(){
		x = y = x2 = y2 = width = height = 0;
	}
	
	public Rectangle(int x, int y, int width, int height){
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		x2 = x + width;
		y2 = y + height;
	}
	
	public int x(){ return x; }
	public int y(){ return y; }
	public int x2(){ return x2; }
	public int y2(){ return y2; }
	public int width(){ return width; }
	public int height(){ return height; }
	
	public String toString(){ return "(" + x + ", " + y + ", " + x2 + ", " + y2 + ")"; }
	
	public void drawMe(Graphics g){
			g.drawRect(x, y, width, height);
	}
}