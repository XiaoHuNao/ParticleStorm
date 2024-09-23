package org.mesdag.particlestorm.data.molang;

import com.mojang.serialization.Codec;
import org.mesdag.particlestorm.data.molang.compiler.MathParser;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

public class MolangExp {
    public static final MolangExp EMPTY = new MolangExp("");
    public static final Codec<MolangExp> CODEC = Codec.STRING.xmap(MolangExp::new, e -> e.expStr);
    protected final String expStr;
    protected MathValue variable;

    public MolangExp(String expStr) {
        this.expStr = expStr;
    }

    public String getExpStr() {
        return expStr;
    }

    public void compile(MathParser parser) {
        if (variable == null && !expStr.isEmpty() && !expStr.isBlank()) {
            this.variable = parser.compileMolang(expStr);
        }
    }

    public float calculate(MolangParticleInstance instance) {
        if (!initialized()) return 0.0F;
        return (float) variable.get(instance);
    }

    public MathValue getVariable() {
        return variable;
    }

    public boolean initialized() {
        return variable != null;
    }
}
