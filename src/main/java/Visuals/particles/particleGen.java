package Visuals.particles;


import java.util.Random;

import Visuals.engine.io.Window;
import org.lwjglx.util.vector.Matrix4f;
import org.lwjglx.util.vector.Vector3f;
import org.lwjglx.util.vector.Vector4f;


public class particleGen {

    private float pps, averageSpeed, gravityComplient, averageLifeLength, averageScale;

    private float speede=0;
    private float lifee=0;
    private float scalee = 0;
    private boolean randomRotation = false;
    private Vector3f direction;
    private float directionDeviation = 0;
    private ParticleTexture texture;
    private Random random = new Random();

    public particleGen(ParticleTexture texture, float pps, float speed, float gravityComplient, float lifeLength, float scale) {
        this.texture = texture;
        this.pps = pps;
        this.averageSpeed = speed;
        this.gravityComplient = gravityComplient;
        this.averageLifeLength = lifeLength;
        this.averageScale = scale;
    }

    /**
     * @param direction - The average direction in which particles are emitted.
     * @param deviation - A value between 0 and 1 indicating how far from the chosen direction particles can deviate.
     */
    public void setDirection(Vector3f direction, float deviation) {
        this.direction = new Vector3f(direction);
        this.directionDeviation = (float) (deviation * Math.PI);
    }

    public void randomizeRotation() {
        randomRotation = true;
    }


    public void setSpeede(float e) {
        this.speede = e * averageSpeed;
    }


    public void setLifee(float e) {
        this.lifee = e * averageLifeLength;
    }


    public void setScalee(float e) {
        this.scalee = e * averageScale;
    }



    public void generateParticles(Vector3f systemCenter) {
        float delta = Window.getFrameTimeSeconds();
        float particlesToCreate = pps * delta;
        int count = (int) Math.floor(particlesToCreate);
        float partialParticle = particlesToCreate % 1;
        for (int i = 0; i < count; i++) {
            emitParticle(systemCenter);
        }
        if (Math.random() < partialParticle) {
            emitParticle(systemCenter);
        }
    }

    private void emitParticle(Vector3f center) {
        Vector3f velocity = null;
        if(direction!=null){
            velocity = genRandomUnitVectorInCone(direction, directionDeviation);
        }else{
            velocity = genRandomUnitVector();
        }
        velocity.normalise();
        velocity.scale(generateValue(averageSpeed, speede));
        float scale = generateValue(averageScale, scalee);
        float lifeLength = generateValue(averageLifeLength, lifee);
        new Particle(texture,new Vector3f(center), velocity, gravityComplient, lifeLength, generateRotation(), scale);
    }

    private float generateValue(float average, float errorMargin) {
        float offset = (random.nextFloat() - 0.5f) * 2f * errorMargin;
        return average + offset;
    }

    private float generateRotation() {
        if (randomRotation) {
            return random.nextFloat() * 360f;
        } else {
            return 0;
        }
    }

    private static Vector3f genRandomUnitVectorInCone(Vector3f dir, float angle) {
        float cosAngle = (float) Math.cos(angle);
        Random random = new Random();
        float theta = (float) (random.nextFloat() * 2f * Math.PI);
        float z = cosAngle + (random.nextFloat() * (1 - cosAngle));
        float rootOneMinusZSquared = (float) Math.sqrt(1 - z * z);
        float x = (float) (rootOneMinusZSquared * Math.cos(theta));
        float y = (float) (rootOneMinusZSquared * Math.sin(theta));

        Vector4f direction = new Vector4f(x, y, z, 1);
        if (dir.x != 0 || dir.y != 0 || (dir.z != 1 && dir.z != -1)) {
            Vector3f axis = Vector3f.cross(dir, new Vector3f(0, 0, 1), null);
            axis.normalise();
            float rotateAngle = (float) Math.acos(Vector3f.dot(dir, new Vector3f(0, 0, 1)));
            Matrix4f rotationMatrix = new Matrix4f();
            rotationMatrix.rotate(-rotateAngle, axis);
            Matrix4f.transform(rotationMatrix, direction, direction);
        } else if (dir.z == -1) {
            direction.z *= -1;
        }
        return new Vector3f(direction);
    }

    private Vector3f genRandomUnitVector() {
        float theta = (float) (random.nextFloat() * 2f * Math.PI);
        float z = (random.nextFloat() * 2) - 1;
        float rootOneMinusZSquared = (float) Math.sqrt(1 - z * z);
        float x = (float) (rootOneMinusZSquared * Math.cos(theta));
        float y = (float) (rootOneMinusZSquared * Math.sin(theta));
        return new Vector3f(x, y, z);
    }

}