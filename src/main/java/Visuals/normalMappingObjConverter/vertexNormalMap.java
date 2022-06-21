package Visuals.normalMappingObjConverter;

import java.util.ArrayList;
import java.util.List;

import org.lwjglx.util.vector.Vector3f;

public class vertexNormalMap {
	
	private static final int remove = -1;
	
	private Vector3f position;
	private int textureIndex = remove;
	private int normalIndex = remove;
	private vertexNormalMap duplicateVertex = null;
	private int index;
	private float length;
	private List<Vector3f> tangents = new ArrayList<Vector3f>();
	private Vector3f averagedTangent = new Vector3f(0, 0, 0);
	
	protected vertexNormalMap(int index, Vector3f position){
		this.index = index;
		this.position = position;
		this.length = position.length();
	}
	
	protected void addTangent(Vector3f tangent){
		tangents.add(tangent);
	}

	protected vertexNormalMap duplicate(int newIndex){
		vertexNormalMap vertex = new vertexNormalMap(newIndex, position);
		vertex.tangents = this.tangents;
		return vertex;
	}
	
	protected void avgTan(){
		if(tangents.isEmpty()){
			return;
		}
		for(Vector3f tangent : tangents){
			Vector3f.add(averagedTangent, tangent, averagedTangent);
		}
		averagedTangent.normalise();
	}
	
	protected Vector3f getAverageTangent(){
		return averagedTangent;
	}
	
	protected int getIndex(){
		return index;
	}
	
	protected float getLength(){
		return length;
	}
	
	protected boolean isSet(){
		return textureIndex!= remove && normalIndex!= remove;
	}
	
	protected boolean hasSameTextureAndNormal(int textureIndexOther,int normalIndexOther){
		return textureIndexOther==textureIndex && normalIndexOther==normalIndex;
	}
	
	protected void setTextureIndex(int textureIndex){
		this.textureIndex = textureIndex;
	}
	
	protected void setNormalIndex(int normalIndex){
		this.normalIndex = normalIndex;
	}

	protected Vector3f getPosition() {
		return position;
	}

	protected int getTextureIndex() {
		return textureIndex;
	}

	protected int getNormalIndex() {
		return normalIndex;
	}

	protected vertexNormalMap getDuplicateVertex() {
		return duplicateVertex;
	}

	protected void setDuplicateVertex(vertexNormalMap duplicateVertex) {
		this.duplicateVertex = duplicateVertex;
	}

}
