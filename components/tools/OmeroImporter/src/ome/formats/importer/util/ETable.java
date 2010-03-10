/** ETable.java is a part of the salma-heyek java repository of classes 
 * (found at http://software.jessies.org/salma-hayek/) and is covered by the GNU 
 * lesser general license.
 */

package ome.formats.importer.util;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

/**
 * A better-looking table than JTable. In particular, on Mac OS this looks
 * more like a Cocoa table than the default Aqua LAF manages.
 * 
 * @author Elliott Hughes
 * @author Some additions added by Brian W. Loranger
 */
public class ETable extends JTable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final Color MAC_UNFOCUSED_SELECTED_CELL_BACKGROUND_COLOR = new Color(0xc0c0c0);
	public static final Color MAC_UNFOCUSED_UNSELECTED_VERTICAL_LINE_COLOR = new Color(0xd9d9d9);
	public static final Color MAC_UNFOCUSED_SELECTED_VERTICAL_LINE_COLOR = new Color(0xacacac);

	/**
	 * The background color for alternate rows in lists and tables.
	 */
	public static final Color ALTERNATE_ROW_COLOR = new Color(0.92f, 0.95f, 0.99f);
	public static final Color DARK_ORANGE = new Color(1.0f, 0.4f, 0.0f);

	public ETable() {
		// Although it's the JTable default, most systems' tables don't draw a grid by default.
		// Worse, it's not easy (or possible?) for us to take over grid painting ourselves for those LAFs (Metal, for example) that do paint grids.
		// The Aqua and GTK LAFs ignore the grid settings anyway, so this causes no change there.
		setShowGrid(false);

		// Tighten the cells up, and enable the manual painting of the vertical grid lines.
		setIntercellSpacing(new Dimension());

		// Table column re-ordering is too badly implemented to enable.
		getTableHeader().setReorderingAllowed(false);

		if (isMacOs()) {
			// Work-around for Apple 4352937.
			JLabel.class.cast(getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEADING);

			// Use an iTunes-style vertical-only "grid".
			setShowHorizontalLines(true);
			setShowVerticalLines(true);
		}
	}
	
	public ETable(AbstractTableModel atm) {

		setModel(atm);
		// Although it's the JTable default, most systems' tables don't draw a grid by default.
		// Worse, it's not easy (or possible?) for us to take over grid painting ourselves for those LAFs (Metal, for example) that do paint grids.
		// The Aqua and GTK LAFs ignore the grid settings anyway, so this causes no change there.
		setShowGrid(false);

		// Tighten the cells up, and enable the manual painting of the vertical grid lines.
		setIntercellSpacing(new Dimension());

		// Table column re-ordering is too badly implemented to enable.
		getTableHeader().setReorderingAllowed(false);

		if (isMacOs()) {
			// Work-around for Apple 4352937.
			JLabel.class.cast(getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEADING);

			// Use an iTunes-style vertical-only "grid".
			setShowHorizontalLines(false);
			setShowVerticalLines(true);
		}
	}
	/**
	 * Tests whether we're running on Mac OS. The Mac is quite
	 * different from Linux and Windows, and it's sometimes
	 * necessary to put in special-case behavior if you're running
	 * on the Mac.
	 */
	public static boolean isMacOs() {
		return System.getProperty("os.name").contains("Mac");
	}

	/**
	 * Tests whether we're using the GTK+ LAF (and so are probably on Linux or Solaris).
	 */
	public static boolean isGtk() {
		return UIManager.getLookAndFeel().getClass().getName().contains("GTK");
	}

	/**
	 * Paints empty rows too, after letting the UI delegate do
	 * its painting.
	 */
	public void paint(Graphics g) {
		super.paint(g);
		paintEmptyRows(g);
	}

	/**
	 * Paints the backgrounds of the implied empty rows when the
	 * table model is insufficient to fill all the visible area
	 * available to us. We don't involve cell renderers, because
	 * we have no data.
	 */
	protected void paintEmptyRows(Graphics g) {
		final int rowCount = getRowCount();
		final Rectangle clip = g.getClipBounds();
		final int height = clip.y + clip.height;
		if (rowCount * rowHeight < height) {
			for (int i = rowCount; i <= height/rowHeight; ++i) {
				g.setColor(colorForRow(i));
				g.fillRect(clip.x, i * rowHeight, clip.width, rowHeight);
			}

			// Mac OS' Aqua LAF never draws vertical grid lines, so we have to draw them ourselves.
			if (isMacOs() && getShowVerticalLines()) {
				g.setColor(MAC_UNFOCUSED_UNSELECTED_VERTICAL_LINE_COLOR);
				TableColumnModel columnModel = getColumnModel();
				int x = 0;
				for (int i = 0; i < columnModel.getColumnCount(); ++i) {
					TableColumn column = columnModel.getColumn(i);
					x += column.getWidth();
					if (!(column.getCellRenderer() instanceof JCheckBox))
					    g.drawLine(x - 1, rowCount * rowHeight, x - 1, height);
				}
			}
		}
	}

	/**
	 * Changes the behavior of a table in a JScrollPane to be more like
	 * the behavior of JList, which expands to fill the available space.
	 * JTable normally restricts its size to just what's needed by its
	 * model.
	 */
	public boolean getScrollableTracksViewportHeight() {
		if (getParent() instanceof JViewport) {
			JViewport parent = (JViewport) getParent();
			return (parent.getHeight() > getPreferredSize().height);
		}
		return false;
	}

	/**
	 * Returns the appropriate background color for the given row.
	 */
	public Color colorForRow(int row) {
		return (row % 2 == 0) ? alternateRowColor() : getBackground();
	}

	private Color alternateRowColor() {
		return isGtk() ? Color.WHITE : ALTERNATE_ROW_COLOR;
	}

	public void enableCell(int row, int column, boolean enabled)
	{
        TableCellRenderer renderer = getCellRenderer(row, column);
        Component c = prepareRenderer(renderer, row, column);

        // Now have to see if the component is a JComponent before
        // getting the tip
        if (c instanceof JComponent) {
            c.setEnabled(enabled);
        }
	}
	
	/**
	 * Shades alternate rows in different colors.
	 */
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
		Component c = super.prepareRenderer(renderer, row, column);
		boolean focused = hasFocus();
		boolean selected = isCellSelected(row, column);
		if (selected) {
			if (isMacOs() && focused == false) {
				// Native Mac OS renders the selection differently if the table doesn't have the focus.
				// The Mac OS LAF doesn't imitate this for us.
				c.setBackground(MAC_UNFOCUSED_SELECTED_CELL_BACKGROUND_COLOR);
				c.setForeground(UIManager.getColor("Table.foreground"));
			} else {
				c.setBackground(UIManager.getColor("Table.selectionBackground"));
				c.setForeground(UIManager.getColor("Table.selectionForeground"));
			}
		} else {
			// Outside of selected rows, we want to alternate the background color.
			c.setBackground(colorForRow(row));
			if (c.getForeground() != Color.red && c.getForeground() != DARK_ORANGE)
			    c.setForeground(UIManager.getColor("Table.foreground"));
		}

		if (c instanceof JComponent) {
			JComponent jc = (JComponent) c;
            jc.setOpaque(true);
			// The Java 6 GTK LAF JCheckBox doesn't paint its background by default.
			// Sun 5043225 says this is the intended behavior, though presumably not when it's being used as a table cell renderer.
			if (isGtk() && c instanceof JCheckBox) {
				jc.setOpaque(true);
			}

			if (isMacOs()) {
				// Native Mac OS doesn't draw a border on the selected cell.
				// It does however draw a horizontal line under the whole row, and a vertical line separating each column.
				fixMacOsCellRendererBorder(jc, selected, focused);
			} else {
				// FIXME: doesn't Windows have row-wide selection focus?
				// Hide the cell focus.
				jc.setBorder(null);
			}

			initToolTip(jc, row, column);
		}

		return c;
	}
	
	private void fixMacOsCellRendererBorder(JComponent renderer, boolean selected, boolean focused) {
		Color verticalLineColor = selected ? MAC_UNFOCUSED_SELECTED_VERTICAL_LINE_COLOR : MAC_UNFOCUSED_UNSELECTED_VERTICAL_LINE_COLOR;
		Border border = BorderFactory.createMatteBorder(0, 0, 0, 1, verticalLineColor);
		renderer.setBorder(border);
	}

	/**
	 * Sets the component's tool tip if the component is being rendered smaller than its preferred size.
	 * This means that all users automatically get tool tips on truncated text fields that show them the full value.
	 */
	public void initToolTip(JComponent c, int row, int column) {
		String toolTipText = null;
		if (c.getPreferredSize().width > getCellRect(row, column, false).width) {
			toolTipText = getValueAt(row, column).toString();
		}
		c.setToolTipText(toolTipText);
	}

	/**
	 * Improve the appearance of of a table in a JScrollPane on Mac OS, where there's otherwise an unsightly hole.
	 */
	@Override
	protected void configureEnclosingScrollPane() {
		super.configureEnclosingScrollPane();

		if (isMacOs() == false) {
			return;
		}

		Container p = getParent();
		if (p instanceof JViewport) {
			Container gp = p.getParent();
			if (gp instanceof JScrollPane) {
				JScrollPane scrollPane = (JScrollPane)gp;
				// Make certain we are the viewPort's view and not, for
				// example, the rowHeaderView of the scrollPane -
				// an implementor of fixed columns might do this.
				JViewport viewport = scrollPane.getViewport();
				if (viewport == null || viewport.getView() != this) {
					return;
				}

				// JTable copy & paste above this point; our code below.

				// Put a dummy header in the upper-right corner.
				final Component renderer = new JTableHeader().getDefaultRenderer().getTableCellRendererComponent(null, "", false, false, -1, 0);
				JPanel panel = new JPanel(new BorderLayout());
				panel.add(renderer, BorderLayout.CENTER);
				scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, panel);
			}
		}
	}
}
