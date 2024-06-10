/*
 *     MCEF (Minecraft Chromium Embedded Framework)
 *     Copyright (C) 2024 Ruxy
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 */

package net.rk4z.mcef.progress;

import net.rk4z.mcef.MCEF;

public class MCEFProgressTracker {

    private String task;
    private float percent;
    private boolean done;

    private float loggedPercent;

    public void setTask(String name) {
        this.task = name;
        this.percent = 0;

        MCEF.INSTANCE.getLogger().info("[" + this.task + "] Started task");
    }

    public String getTask() {
        return task;
    }

    public void setProgress(float percent) {
        this.percent = Math.min(1, Math.max(0, percent));

        if ((int) (this.percent * 100) != (int) (loggedPercent * 100)) {
            MCEF.INSTANCE.getLogger().info("[" + this.task + "] Progress " + (int) (this.percent * 100) + "%");
            this.loggedPercent = this.percent;
        }
    }

    public float getProgress() {
        return percent;
    }

    public void done() {
        this.done = true;
        MCEF.INSTANCE.getLogger().info("[" + this.task + "] Finished task");
    }

    public boolean isDone() {
        return done;
    }

}
