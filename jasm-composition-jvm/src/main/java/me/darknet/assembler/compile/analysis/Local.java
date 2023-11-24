package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.PrimitiveKind;
import dev.xdark.blw.type.PrimitiveType;
import org.jetbrains.annotations.NotNull;

public class Local {
	protected final int index;
	protected final String name;
	protected final ClassType type;

	public Local(int index, @NotNull String name, @NotNull ClassType type) {
		this.index = index;
		this.name = name;
		this.type = type;
	}

	public int index() {
		return index;
	}

	@NotNull
	public String name() {
		return name;
	}

	@NotNull
	public ClassType type() {
		return type;
	}

	@NotNull
	public Local adaptType(@NotNull ClassType newType) {
		return new Local(index, name, newType);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Local local = (Local) o;

		if (index != local.index) return false;
		if (!name.equals(local.name)) return false;
		return type.equals(local.type);
	}

	@Override
	public int hashCode() {
		int result = index;
		result = 31 * result + name.hashCode();
		result = 31 * result + type.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "Local{" +
				"index=" + index +
				", name='" + name + '\'' +
				", type=" + type +
				'}';
	}

	/**
	 * @return Type size of this variable.
	 * {@code 1} for all types except for {@link double}/{@code long} which is {@code 2}.
	 */
	public int size() {
		if (type instanceof PrimitiveType primitiveType) {
			int kind = primitiveType.kind();
			return (kind == PrimitiveKind.T_LONG || kind == PrimitiveKind.T_DOUBLE) ? 2 : 1;
		}
		return 1;
	}
}
