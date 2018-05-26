package io.github.lhanson.possum.entity

import io.github.lhanson.possum.component.*
import io.github.lhanson.possum.component.layout.PaddingComponent
import io.github.lhanson.possum.component.layout.RelativeAreaComponent
import io.github.lhanson.possum.component.layout.RelativePositionComponent
import io.github.lhanson.possum.events.SceneInitializedEvent
import io.github.lhanson.possum.events.Subscription
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static io.github.lhanson.possum.component.AreaComponent.FrameOfReference.ASCII_PANEL
import static io.github.lhanson.possum.component.AreaComponent.FrameOfReference.PARENT

/**
 * Represents a visible UI container with a border, and can be specifically sized
 * or stretch to the area of its contents with optional padding.
 *
 * AreaComponent, InventoryComponent, and PaddingComponent are guaranteed to be present,
 * so defaults are generated if required.
 *
 * InventoryComponents' areas are relative to the panel's inner dimensions with no
 * reference to borders or padding, e.g. an inventory item at Y == 0 might actually
 * be several locations units below the top edge.
 *
 * Default padding is 1; this allows borders to be rendered without panel content
 * overlapping them.
 */
class PanelEntity extends GameEntity {
	static final int DEFAULT_PADDING = 1
	Logger log = LoggerFactory.getLogger(this.class)
	// Local references to components we need easy access to
	protected AreaComponent area
	protected PaddingComponent padding
	InventoryComponent inventory

	PanelEntity() {
		this(null)
	}

	PanelEntity(GameEntity gameEntity) {
		this([gameEntity])
	}

	PanelEntity(List<GameEntity> panelEntities) {
		super()
		this.inventory = new InventoryComponent(panelEntities)
		components.add(inventory)
	}

	@Override
	void setComponents(List<GameComponent> components) {
		println "Setting components: $components"
		super.setComponents(components)
		ensurePaddingComponent()
		ensureAreaComponent()
		ensureInventoryComponent()
		ensureRelativePosition()
		computeInventoryPositions()
		computeArea()
	}

//	@Override
//	boolean addComponentInternal(GameComponent component, boolean publishEvent) {
//		println "---Adding  $component, computing internal positions"
	// TODO: computeInventoryPositions needs to happen even if we don't call addComponents;
	// TODO: do we do it each time, on scene initialization, when?
//		computeInventoryPositions()
//		super.addComponentInternal(component, publishEvent)
//	}

	@Subscription
	void sceneInitialized(SceneInitializedEvent event) {
		computeInventoryPositions()
	}

	AreaComponent getArea() {
		getComponentOfType(AreaComponent)
	}

	PaddingComponent getPadding() {
		getComponentOfType(PaddingComponent)
	}

	@Override
	GameComponent getComponentOfType(Class requiredType) {
		def result = super.getComponentOfType(requiredType)
		if (!result && requiredType == InventoryComponent) {
			result = ensureInventoryComponent()
		} else if (!result && requiredType == PaddingComponent) {
			result = ensurePaddingComponent()
		} else if (!result && requiredType == AreaComponent) {
			result = ensureAreaComponent()
		}
		return result
	}

	/**
	 * For panels without fixed dimensions, recalculate area based on current inventory
	 */
	protected void computeArea() {
		ensurePaddingComponent()
		RelativeAreaComponent rac = getComponentOfType(RelativeAreaComponent)
		int width = 0
		int height = 0
		inventory.each { GameEntity e ->
			AreaComponent itemArea = e.getComponentOfType(AreaComponent)
			if (itemArea.width > width) {
				width = itemArea.width
			}
			height++
		}
		if (rac?.width == null && rac?.relativeWidth == null) {
			// Shrink-wrap horizontally if no width is specified
			area.width = width + padding.width
		} else if (rac?.width) {
			// Set absolute width if specified
			area.width = rac.width
		}

		if (rac?.height == null && rac?.relativeHeight == null) {
			// Shrink-wrap vertically if no width is specified
			area.height = height + padding.height
		} else if (rac?.height) {
			// Set absolute height if specified
			area.height = rac.height
		}
		log.debug "Computed area for panel '{}' with {} inventory items and a padding value of {}",
				this, inventory.size(), padding
	}

	// We guarantee an InventoryComponent is present, create one if needed
	private InventoryComponent ensureInventoryComponent() {
		if (inventory == null) {
			log.debug "No InventoryComponent found for panel entity $name on initialization, adding one"
			inventory = new InventoryComponent()
			components << inventory
		}
		if (!area) {
			ensureAreaComponent()
			computeArea()
		}
		return inventory
	}

	// We guarantee an AreaComponent is present, create one if needed
	private AreaComponent ensureAreaComponent() {
		AreaComponent ac = super.getComponentOfType(AreaComponent)
		if (!ac) {
			log.debug "No AreaComponent found for panel entity '$name' on initialization, adding one"
			ac = new AreaComponent()
			components << ac
		}
		ac.frameOfReference = ASCII_PANEL
		area = ac
		if (inventory == null) {
			ensureInventoryComponent()
		}
		computeArea()
		return area
	}

	// We guarantee a PaddingComponent is present, create one if needed
	private PaddingComponent ensurePaddingComponent() {
		PaddingComponent pc = super.getComponentOfType(PaddingComponent)
		if (!pc) {
			log.debug "No PanelComponent found for panel entity '$name' on initialization, adding one"
			pc = new PaddingComponent(DEFAULT_PADDING)
			components << pc
		}
		padding = pc
		return padding
	}

	// RelativeAreaComponent must be accompanied by a RelativePositionComponent
	private void ensureRelativePosition() {
		if (getComponentOfType(RelativeAreaComponent) &&
				!getComponentOfType(RelativePositionComponent)) {
			log.debug "Found RelativeAreaComponent but no RelativePositionComponent for panel $name on initialization, adding one"
			components.add(new RelativePositionComponent(50, 50))
		}
	}

	// For panel inventory entities not already placed via relative positioning,
	// we compute their vertical order and take any padding into account.
	void computeInventoryPositions() {
		println "Computing inventory positions"
		inventory.eachWithIndex { entity, i ->
			AreaComponent ac = entity.getComponentOfType(AreaComponent)
			ac.frameOfReference = PARENT
			if (!entity.getComponentOfType(RelativePositionComponent)) {
				entity.getComponentOfType(AreaComponent)?.x = padding.left
				entity.getComponentOfType(AreaComponent)?.y = padding.top + i
			}
		}
	}

}
