package graphics;
//~ import java.awt.Graphics;
import java.awt.Rectangle;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.events.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;


//~ abstract class GmmlDrawingObject extends JComponent 
abstract class GmmlDrawingObject implements Comparable
{
	boolean isSelected;
	int drawingOrder = 2;
	
	/**
	 * Draws the GmmlDrawingObject object on the GmmlDrawing
	 * it is part of
	 * @param g - the Graphics object to use for drawing
	 */
	protected abstract void draw(PaintEvent e, GC buffer);
	protected abstract void draw(PaintEvent e);
	/**
	 * Determines wheter a GmmlGraphics object contains
	 * the point specified
	 * @param point - the point to check
	 * @return True if the object contains the point, false otherwise
	 */
	protected abstract boolean isContain(Point2D point);
	
	/**
	 * Determines whether a GmmlGraphics object intersects 
	 * the rectangle specified
	 * @param r - the rectangle to check
	 * @return True if the object intersects the rectangle, false otherwise
	 */
	protected abstract boolean intersects(Rectangle2D.Double r);
	
	protected abstract Rectangle getBounds();
	/**
	 * Moves GmmlGraphics object by specified increments
	 * @param dx - the value of x-increment
	 * @param dy - the value of y-increment
	 */
	void moveBy(double dx, double dy) {}
	
	public int compareTo(Object o) throws ClassCastException
	{
		if(!(o instanceof GmmlDrawingObject))
		{
			throw new ClassCastException("Object is not of type GmmlDrawingObject");
		}
		GmmlDrawingObject d = ((GmmlDrawingObject)o);
		if(isSelected && !d.isSelected && !(d instanceof GmmlHandle))
		{
			return 1;
		}
		if(d.isSelected && !isSelected && !(this instanceof GmmlHandle))
		{
			return -1;
		}
		return d.drawingOrder - drawingOrder; //Lowest index sorted last
	}
	
}
