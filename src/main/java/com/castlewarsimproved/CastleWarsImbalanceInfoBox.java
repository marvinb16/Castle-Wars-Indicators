package com.castlewarsimproved;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxPriority;

import java.awt.*;
import java.awt.image.BufferedImage;

public class CastleWarsImbalanceInfoBox extends InfoBox
{
    private String label = "Balanced";

    public CastleWarsImbalanceInfoBox(BufferedImage image, Plugin owner)
    {
        super(image, owner);
        setPriority(InfoBoxPriority.MED);
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    @Override
    public String getText()
    {
        return label;
    }

    @Override
    public Color getTextColor() {
        return null;
    }

    @Override
    public String getTooltip()
    {
        return "Castle Wars balance indicator";
    }
}
