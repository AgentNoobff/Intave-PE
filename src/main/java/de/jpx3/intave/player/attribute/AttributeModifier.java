package de.jpx3.intave.player.attribute;

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;
import de.jpx3.intave.share.MinecraftKey;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class AttributeModifier {
	private final MinecraftKey key;
	private final UUID uuid;
	private final String name;
	private final Operation operation;
	private final double amount;

	public AttributeModifier(
		MinecraftKey key, UUID uuid,
		String name, Operation operation,
		double amount
	) {
		this.key = key;
		this.uuid = uuid;
		this.name = name;
		this.operation = operation;
		this.amount = amount;
	}

	public MinecraftKey key() {
		return key;
	}

	public UUID id() {
		return uuid;
	}

	public String name() {
		return name;
	}

	public Operation operation() {
		return operation;
	}

	public double amount() {
		return amount;
	}

	public static Set<AttributeModifier> fromProtocolLib(
		List<WrapperPlayServerUpdateAttributes.PropertyModifier> protocolLibModifiers
	) {
		return protocolLibModifiers.stream()
			.map(AttributeModifier::fromProtocolLib)
			.collect(Collectors.toSet());
	}

	private static AttributeModifier fromProtocolLib(
		WrapperPlayServerUpdateAttributes.PropertyModifier protocolLibModifier
	) {
		com.github.retrooper.packetevents.resources.ResourceLocation name = protocolLibModifier.getName();
		MinecraftKey key = name != null
			? MinecraftKey.fromProtocolLib(name)
			: MinecraftKey.withDefaultNamespace("unknown");
		String displayName = name != null ? name.getKey() : "unknown";
		return new AttributeModifier(
			key,
			protocolLibModifier.getUUID(),
			displayName,
			Operation.fromId(protocolLibModifier.getOperation().ordinal()),
			protocolLibModifier.getAmount()
		);
	}

	@Override
	public String toString() {
		return "WrappedAttributeModifier{" +
			"key=" + key +
			", uuid=" + uuid +
			", name='" + name + '\'' +
			", operation=" + operation +
			", amount=" + amount +
			'}';
	}

	public static Builder newBuilder(UUID uuid) {
		return new Builder(uuid);
	}

	public static class Builder {
		private final UUID uuid;
		private MinecraftKey key;
		private String name;
		private Operation operation;
		private double amount;

		public Builder(UUID uuid) {
			this.uuid = uuid;
		}

		public Builder withKey(MinecraftKey key) {
			this.key = key;
			return this;
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder withOperation(Operation operation) {
			this.operation = operation;
			return this;
		}

		public Builder withAmount(double amount) {
			this.amount = amount;
			return this;
		}

		public AttributeModifier build() {
			if (name == null || operation == null) {
				throw new IllegalStateException("Key, name, and operation must be set");
			}
			if (key == null) {
				key = new MinecraftKey("intave", "custom_modifier");
			}
			return new AttributeModifier(key, uuid, name, operation, amount);
		}
	}

	public enum Operation {
		ADD_NUMBER(0),
		MULTIPLY_PERCENTAGE(1),
		ADD_PERCENTAGE(2);

		private final int id;

		Operation(int id) {
			this.id = id;
		}

		public int getId() {
			return this.id;
		}

		public static Operation fromId(int id) {
			for (Operation op : values()) {
				if (op.getId() == id) {
					return op;
				}
			}
			throw new IllegalArgumentException("Invalid operation id: " + id);
		}
	}
}
