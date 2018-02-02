package io.github.lhanson.possum.component

import spock.lang.Specification

import static io.github.lhanson.possum.component.AreaComponent.FrameOfReference.WORLD

class AreaComponentTest extends Specification {

	def "basic equality"() {
		when:
			def area1 = new AreaComponent(0, 0, 1, 1)
			def area2 = new AreaComponent(0, 0, 1, 1)
		then:
			area1.x == area2.x
			area1.y == area2.y
			area1.width == area2.width
			area1.height == area2.height
			area1.equals(area2)
			area1 == area2
	}

	def "Same area overlaps"() {
		when:
			def area1 = new AreaComponent(0, 0, 1, 1)
			def area2 = new AreaComponent(0, 0, 1, 1)
		then:
			area1.overlaps(area2)
	}

	def "Same area overlaps with ignored z-component"() {
		when:
			def area1 = new AreaComponent(0, 0, 0, 1, 1)
			def area2 = new AreaComponent(0, 0, 1, 1, 1)
		then:
			area1.overlaps(area2)
	}

	def "No overlap"() {
		when:
			def area1 = new AreaComponent(0, 0, 1, 1)
			def area2 = new AreaComponent(10, 10, 1, 1)
		then:
			!area1.overlaps(area2)
	}

	def "Adjacent but no overlap"() {
		when:
			def panel = new AreaComponent(0, 0, 1, 1)
			def entity = new AreaComponent(0, 1, 1, 1)
		then:
			!entity.overlaps(panel)
	}

	def "Overlap horizontally"() {
		when:
			def area1 = new AreaComponent(0, 0, 5, 1)
			def area2 = new AreaComponent(-3, 0, 11, 1)
		then:
			area1.overlaps(area2)
	}

	def "Determining if an area is completely contained by another"() {
		when:
			def bigArea = new AreaComponent(0, 0, 100, 100)
			def internalArea = new AreaComponent(10, 10, 10, 10)
			def overlappingArea = new AreaComponent(50, 50, 100, 100)

		then:
			bigArea.overlaps(internalArea)
			bigArea.overlaps(overlappingArea)

			bigArea.union(internalArea) == internalArea
			bigArea.contains(internalArea)

			bigArea.union(overlappingArea) != overlappingArea
			!bigArea.contains(overlappingArea)
	}

	def "Union of same area"() {
		given:
			def area1 = new AreaComponent(0, 0, 4,4)
			def area2 = new AreaComponent(0, 0, 4,4)
		when:
			def union = area1.union(area2)
		then:
			union == area1
			union == area2
	}

	def "Union of same area ignores z-component"() {
		given:
			def area1 = new AreaComponent(0, 0, 0, 4,4)
			def area2 = new AreaComponent(0, 0, 1, 4,4)
		when:
			def union = area1.union(area2)
		then:
			union == area1
			union == area2
	}

	def "Union with x overlap"() {
		given:
			def area1 = new AreaComponent(0, 0, 4, 1)
			def area2 = new AreaComponent(1, 0, 4, 1)
		when:
			def union = area1.union(area2)
		then:
			union == new AreaComponent(1, 0, 3, 1)
	}

	def "Union with y overlap"() {
		given:
			def area1 = new AreaComponent(0, 0, 4, 4)
			def area2 = new AreaComponent(0, 2, 4, 4)
		when:
			def union = area1.union(area2)
		then:
			union == new AreaComponent(0, 2, 4, 2)
	}

	def "Union with x and y overlap"() {
		given:
			def area1 = new AreaComponent(0, 0, 4, 4)
			def area2 = new AreaComponent(1, 1, 4, 4)
		when:
			def union = area1.union(area2)
		then:
			union == new AreaComponent(1, 1, 3, 3)
	}

	def "Clip where subject is wholly inside"() {
		given:
			def clipArea = new AreaComponent(0, 0, 4, 4)
			def subjectArea = new AreaComponent(1, 1, 1, 1)
		when:
			AreaComponent clipped = clipArea.clip(subjectArea)
		then:
			clipped == subjectArea
	}

	def "Clip where subject overlaps above"() {
		given:
			def clipArea = new AreaComponent(0, 0, 4, 4)
			def subjectArea = new AreaComponent(0, -1, 2, 2)
		when:
			AreaComponent clipped = clipArea.clip(subjectArea)
		then:
			clipped == new AreaComponent(0, 0, 2, 1)
	}

	def "Clip where subject overlaps below"() {
		given:
			def clipArea = new AreaComponent(0, 0, 4, 4)
			def subjectArea = new AreaComponent(0, 1, 4, 4)
		when:
			AreaComponent clipped = clipArea.clip(subjectArea)
		then:
			clipped == new AreaComponent(0, 1, 4, 3)
	}

	def "Clip where subject overlaps left"() {
		given:
			def clipArea = new AreaComponent(0, 0, 4, 4)
			def subjectArea = new AreaComponent(-1, 0, 4, 4)
		when:
			AreaComponent clipped = clipArea.clip(subjectArea)
		then:
			clipped == new AreaComponent(0, 0, 3, 4)
	}

	def "Clip where subject overlaps right"() {
		given:
			def clipArea = new AreaComponent(0, 0, 4, 4)
			def subjectArea = new AreaComponent(1, 0, 4, 4)
		when:
			AreaComponent clipped = clipArea.clip(subjectArea)
		then:
			clipped == new AreaComponent(1, 0, 3, 4)
	}

	def "Single area subtraction with null parameter returns original area"() {
		given:
			def clipArea = new AreaComponent(0, 0, 4, 4)
		when:
			Set<AreaComponent> remainders = clipArea.subtract(null)
		then:
			remainders == [clipArea] as Set
	}

	def "Multiple area subtraction with null list parameter returns original area"() {
		given:
			def clipArea = new AreaComponent(0, 0, 4, 4)
		when:
			Set<AreaComponent> remainders = clipArea.subtractAll(null)
		then:
			remainders == [clipArea] as Set
	}

	def "Single subtraction outside clip area returns original area"() {
		given:
			def clipArea = new AreaComponent(0, 0, 4, 4)
			def subtractArea = new AreaComponent(6, 0, 1, 1)
		when:
			Set<AreaComponent> remainders = clipArea.subtract(subtractArea)
		then:
			remainders == [clipArea] as Set
	}

	def "Multiple area subtraction outside clip area returns original area"() {
		given:
			def clipArea = new AreaComponent(0, 0, 4, 4)
			def subtractAreas = [
					new AreaComponent(6, 0, 1, 1),
					new AreaComponent(6, 2, 1, 1)
			]
		when:
			Set<AreaComponent> remainders = clipArea.subtractAll(subtractAreas)
		then:
			remainders == [clipArea] as Set
	}

	def "Single subtraction with full overlap returns no area"() {
		given:
			def clipArea = new AreaComponent(0, 0, 4, 4)
			def subtractArea = new AreaComponent(-1, -1, 6, 6)
		when:
			Set<AreaComponent> remainders = clipArea.subtract(subtractArea)
		then:
			remainders == [] as Set
	}

	def "4x4 area, remove 1x1 upper-left corner"() {
		given:
			def clipArea = new AreaComponent(10, 10, 4, 4)
			def sa = new AreaComponent(10, 10, 1, 1)
		when:
			Set<AreaComponent> remainders = clipArea.subtract(sa)
		then:
			remainders == [
					new AreaComponent(11, 10, 3, 1),
					new AreaComponent(10, 11, 4, 3)
			] as Set
	}

	def "4x4 area, remove 1x1 upper-right corner"() {
		given:
			def clipArea = new AreaComponent(10, 10, 4, 4)
			def sa = new AreaComponent(13, 10, 1, 1)
		when:
			Set<AreaComponent> remainders = clipArea.subtract(sa)
		then:
			remainders == [
					new AreaComponent(10, 10, 3, 1),
					new AreaComponent(10, 11, 4, 3)
			] as Set
	}

	def "4x4 area, remove 1x1 lower-left corner"() {
		given:
			def clipArea = new AreaComponent(10, 10, 4, 4)
			def sa = new AreaComponent(10,13, 1, 1)
		when:
			Set<AreaComponent> remainders = clipArea.subtract(sa)
		then:
			remainders == [
					new AreaComponent(10, 10, 4, 3),
					new AreaComponent(11, 13, 3, 1)
			] as Set
	}

	def "4x4 area, remove 1x1 hole in middle"() {
		given:
			def clipArea = new AreaComponent(10, 10, 4, 4)
			def sa = new AreaComponent(11,11, 1, 1)
		when:
			Set<AreaComponent> remainders = clipArea.subtract(sa)
		then:
			remainders == [
					new AreaComponent(10, 10, 4, 1),
					new AreaComponent(10, 11, 1, 1),
					new AreaComponent(12, 11, 2, 1),
					new AreaComponent(10,12, 4, 2)
			] as Set
	}

	def "4x4 area, remove 4x4 partially overlapping area"() {
		given:
			def clipArea = new AreaComponent(10, 10, 4, 4)
			def sa = new AreaComponent(12, 8, 4, 4)
		when:
			Set<AreaComponent> remainders = clipArea.subtract(sa)
		then:
			remainders == [
					new AreaComponent(10, 10, 2, 2),
					new AreaComponent(10, 12, 4, 2)
			] as Set
	}

	def "4x4 area, remove top corners"() {
		given:
			def clipArea = new AreaComponent(10, 10, 4, 4)
			def topLeft = new AreaComponent(10, 10, 1, 1)
			def topRight = new AreaComponent(13, 10, 1, 1)
		when:
			Set<AreaComponent> remainders = clipArea.subtractAll([topLeft, topRight])
		then:
			remainders == [
					new AreaComponent(11, 10, 2, 1),
					new AreaComponent(10, 11, 4, 3)
			] as Set
	}

	def "Subtracting a large area from a small, fully-contained one results in an empty set"() {
		given:
			def largeArea = new AreaComponent(10, 10, 9, 1)
			def smallArea = new AreaComponent(10, 10, 5, 1)
		when:
			def remainders = smallArea.subtract(largeArea)
		then:
			remainders.empty
	}

	def "Subtraction from an area of zero width or height yields nothing"() {
		given:
			def area1 = new AreaComponent(0, 0, 0, 0)
			def area2 = new AreaComponent(0, 0, 5, 1)
		when:
			def remainders = area1.subtract(area2)
		then:
			remainders.empty
	}

	def "AreaComponent defaults to referencing world coordinates"() {
		when:
			def area = new AreaComponent()
		then:
			area.frameOfReference == WORLD
	}

}
