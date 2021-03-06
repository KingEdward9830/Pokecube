package pokecube.core.client.models;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

public class ModelRing extends ModelBase
{
    ResourceLocation texture = new ResourceLocation("pokecube_adventures", "textures/megaring.png");
    //fields
      ModelRenderer Shape2;
      ModelRenderer Shape1;
      ModelRenderer Shape3;
      ModelRenderer Shape4;
      ModelRenderer Shape5;
    
    public ModelRing()
    {
      textureWidth = 64;
      textureHeight = 32;
      
        Shape2 = new ModelRenderer(this, 0, 11);
        Shape2.addBox(0F, 0F, 0F, 1, 1, 1);
        Shape2.setRotationPoint(8.4F, 9F, -0.4333333F);
        Shape2.setTextureSize(64, 32);
        Shape2.mirror = true;
        setRotation(Shape2, 0F, 0F, 0F);
        Shape1 = new ModelRenderer(this, 0, 0);
        Shape1.addBox(0F, 0F, 0F, 1, 1, 4);
        Shape1.setRotationPoint(8F, 9F, -2F);
        Shape1.setTextureSize(64, 32);
        Shape1.mirror = true;
        setRotation(Shape1, 0F, 0F, 0F);
        Shape3 = new ModelRenderer(this, 11, 0);
        Shape3.addBox(0F, 0F, 0F, 6, 1, 1);
        Shape3.setRotationPoint(3F, 9F, -3F);
        Shape3.setTextureSize(64, 32);
        Shape3.mirror = true;
        setRotation(Shape3, 0F, 0F, 0F);
        Shape4 = new ModelRenderer(this, 31, 0);
        Shape4.addBox(0F, 0F, 0F, 1, 1, 5);
        Shape4.setRotationPoint(3F, 9F, -2F);
        Shape4.setTextureSize(64, 32);
        Shape4.mirror = true;
        setRotation(Shape4, 0F, 0F, 0F);
        Shape5 = new ModelRenderer(this, 17, 5);
        Shape5.addBox(0F, 0F, 0F, 5, 1, 1);
        Shape5.setRotationPoint(4F, 9F, 2F);
        Shape5.setTextureSize(64, 32);
        Shape5.mirror = true;
        setRotation(Shape5, 0F, 0F, 0F);
    }
    
    @Override
    public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5)
    {
      super.render(entity, f, f1, f2, f3, f4, f5);
      Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
      GL11.glPushMatrix();
      GL11.glRotated(90, 0, 1, 0);
      GL11.glTranslated(-0.275, -0.5, 0);
      
      setRotationAngles(f, f1, f2, f3, f4, f5, entity);
      Shape2.render(f5);
      Shape1.render(f5);
      Shape3.render(f5);
      Shape4.render(f5);
      Shape5.render(f5);
      
      GL11.glPopMatrix();
    }
    
    private void setRotation(ModelRenderer model, float x, float y, float z)
    {
      model.rotateAngleX = x;
      model.rotateAngleY = y;
      model.rotateAngleZ = z;
    }

    @Override
    public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5, Entity e)
    {
      super.setRotationAngles(f, f1, f2, f3, f4, f5, e);
    }

  }