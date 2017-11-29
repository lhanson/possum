package io.github.lhanson.possum_util.terrain

import io.github.lhanson.possum.terrain.cave.CellularAutomatonCaveGenerator

import javax.imageio.ImageIO
import java.awt.Color
import java.awt.Desktop
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage

/**
 * Test harness for generating and analyzing cellular automata grids.
 */
class CellularAutomataHarness {
	static void main(String[] args) {
		new CellularAutomataHarness().run(10)
	}

	void run(int smoothingGenerations) {
		CellularAutomatonCaveGenerator caveGenerator = new CellularAutomatonCaveGenerator(
				rand: new Random(1),
				width: 200,
				height: 200,
				initialFactor: 50)
		caveGenerator.generate(smoothingGenerations)
		display("ca-cavegen-smoothed ($smoothingGenerations-${caveGenerator.rand.seed})", caveGenerator.grid)

		caveGenerator.analyzeGrid()

		// Fill each room with a distinct color
		def rooms = caveGenerator.countRooms()
		rooms.eachWithIndex { List roomCells, int i ->
			roomCells.each { caveGenerator.grid[it.x][it.y] = i + 1 }
		}
		println "Grid contains ${rooms.size()} distinct rooms"
		display("ca-cavegen-flooded ($smoothingGenerations-${caveGenerator.rand.seed})", caveGenerator.grid)
	}

	void display(String name, int[][] grid) {
		BufferedImage img = scale(render(grid), 3.0)
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
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
		def colorByValue = [0: Color.WHITE, 1: Color.BLACK]
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int value = grid[x][y]
				Color color = colorByValue[value]
				if (!color) {
					color = new Color(new Random().nextInt())
					colorByValue[value] = color
				}
				img.setRGB(x, y, color.getRGB())
			}
		}
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
