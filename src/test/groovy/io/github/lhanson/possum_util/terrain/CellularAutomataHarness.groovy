package io.github.lhanson.possum_util.terrain

import io.github.lhanson.possum.terrain.cave.CellularAutomatonCaveGenerator

import javax.imageio.ImageIO
import java.awt.Color
import java.awt.Desktop
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage

/**
 * Test harness for generating and analyzing cellular automata grids.
 */
class CellularAutomataHarness {
	// Generation parameters
	static int smoothingGenerations = 10
	static int width = 209
	static int height = 209
	static int initialFactor = 50     // percentage of cells initially alive
	// Rendering parameters
	static int renderCellWidth = 16    // how wide to render each cell
	static int renderCellHeight = 16  // how tall to render each cell
	static double scaleFactor = 1.0   // scale the rendered output

	static void main(String[] args) {
		new CellularAutomataHarness().run()
	}

	void run() {
		CellularAutomatonCaveGenerator caveGenerator = new CellularAutomatonCaveGenerator(
				rand: new Random(1),
				width: width,
				height: height,
				initialFactor: initialFactor)
		caveGenerator.generate(smoothingGenerations)
		display("ca-cavegen-smoothed ($smoothingGenerations-${caveGenerator.rand.seed})", caveGenerator.grid)

		caveGenerator.analyzeGrid()

		// Fill each room with a distinct color
		def rooms = caveGenerator.getRooms()
		rooms.eachWithIndex { List roomCells, int i ->
			roomCells.each { caveGenerator.grid[it.x][it.y] = i + 1 }
		}
		def roomSizes = rooms.collect { it.size() }.sort().reverse()
		println "Grid contains ${rooms.size()} distinct rooms. " +
				"Largest rooms: ${roomSizes.take(5)} cells, smallest: ${roomSizes.takeRight(5)}"
		display("ca-cavegen-flooded ($smoothingGenerations-${caveGenerator.rand.seed})", caveGenerator.grid)
	}

	void display(String name, int[][] grid) {
		BufferedImage img = scale(render(grid), scaleFactor)
		try {
			File temp = File.createTempFile(name, ".png")
			ImageIO.write(img, "png", temp)
			Desktop.getDesktop().open(temp)
		} catch(IOException e) {
			System.out.println(e)
		}
	}

	BufferedImage render(int[][] grid) {
		int width = grid.length
		int height = grid[0].length
		println "Rendering a grid of ${grid.length} x ${grid[0].length} cells into $width x $height pixels"
		BufferedImage img = new BufferedImage(width * renderCellWidth, height * renderCellHeight, BufferedImage.TYPE_INT_ARGB)
		Graphics2D g = img.getGraphics()
		def colorByValue = [0: Color.WHITE, 1: Color.BLACK]
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int value = grid[x][y]
				Color color = colorByValue[value]
				if (!color) {
					color = new Color(new Random().nextInt())
					colorByValue[value] = color
				}
				g.setColor(color)
				g.fillRect(x * renderCellWidth, y * renderCellHeight, renderCellWidth, renderCellHeight)
			}
		}
		g.dispose()
		return img
	}

	BufferedImage scale(BufferedImage orig, double factor) {
		BufferedImage scaled = new BufferedImage(
				(int) (orig.width * factor),
				(int) (orig.height * factor),
				BufferedImage.TYPE_INT_ARGB)
		AffineTransform at = new AffineTransform()
		at.scale(factor, factor)
		AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR)
		return scaleOp.filter(orig, scaled)
	}

}
