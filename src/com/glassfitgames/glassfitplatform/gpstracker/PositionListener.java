package com.glassfitgames.glassfitplatform.gpstracker;

import com.glassfitgames.glassfitplatform.models.Position;

public interface PositionListener {
    public void onPositionChanged(Position position);
}
