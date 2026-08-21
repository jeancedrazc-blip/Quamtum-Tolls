package mcjty.rftoolsbuilder.client;

import mcjty.rftoolsbuilder.BuilderBlockEntity;
import mcjty.rftoolsbuilder.BuilderMenu;
import mcjty.rftoolsbuilder.constructor.client.ui.QuantumButton;
import mcjty.rftoolsbuilder.constructor.client.ui.QuantumUiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Builder/Miner terminal using the same Quantum Tools visual system. */
public final class BuilderScreen extends AbstractContainerScreen<BuilderMenu> {
    private final EditBox[] fields = new EditBox[6];
    private QuantumButton actionButton;
    private QuantumButton stopButton;
    private String validation = "";

    public BuilderScreen(BuilderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 256, 232);
        this.inventoryLabelY = 139;
        this.titleLabelY = 7;
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos, y = topPos;
        actionButton = addRenderableWidget(new QuantumButton(x + 158, y + 28, 39, 18,
                Component.literal("START"), () -> sendButton(0), () -> menu.data().get(2) != 0, QuantumUiTheme.GREEN));
        stopButton = addRenderableWidget(new QuantumButton(x + 201, y + 28, 39, 18,
                Component.literal("STOP"), () -> sendButton(1), () -> false, QuantumUiTheme.RED));

        int[] xs = {6,68,127,186,68,127};
        int[] ys = {68,68,68,68,111,111};
        for (int i=0;i<6;i++) {
            fields[i] = new EditBox(font, x + xs[i], y + ys[i], 55, 16, Component.literal("Builder setting"));
            fields[i].setBordered(false);
            fields[i].setMaxLength(6);
            fields[i].setTextColor(QuantumUiTheme.TEXT);
            fields[i].setValue(Integer.toString(menu.data().get(3+i)));
            addRenderableWidget(fields[i]);
            final int field = i;
            addRenderableWidget(new QuantumButton(x + xs[i] + 38, y + ys[i] + 18, 17, 12,
                    Component.literal("✓"), () -> applyField(field), () -> false, QuantumUiTheme.CYAN));
        }
    }

    private void applyField(int field) {
        try {
            int value = Integer.parseInt(fields[field].getValue().trim());
            if (field < 3) value = Math.max(1, Math.min(512, value));
            else value = Math.max(-16384, Math.min(16384, value));
            fields[field].setValue(Integer.toString(value));
            validation = "";
            sendConfig(field, value);
        } catch (NumberFormatException e) {
            validation = "Invalid whole number";
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateWidgets();
    }

    private void updateWidgets() {
        boolean running = menu.data().get(2) != 0;
        int status = menu.data().get(11);
        if (actionButton != null) actionButton.setMessage(Component.literal(running ? "PAUSE" : status == BuilderBlockEntity.STATUS_PAUSED ? "RESUME" : "START"));
        if (stopButton != null) stopButton.active = status != BuilderBlockEntity.STATUS_IDLE;
    }

    private void sendConfig(int field, int value) {
        int id = BuilderMenu.CONFIG_BASE + field * BuilderMenu.CONFIG_RANGE + (value + BuilderMenu.CONFIG_BIAS);
        sendButton(id);
    }

    private void sendButton(int id) {
        Minecraft mc = minecraft;
        if (mc != null && mc.gameMode != null) mc.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        int x=leftPos,y=topPos;
        QuantumUiTheme.window(gui,x,y,imageWidth,imageHeight);
        QuantumUiTheme.title(gui,font,Component.literal("BUILDER // MINING TERMINAL"),x+imageWidth/2,y+7);
        gui.fill(x+8,y+22,x+imageWidth-8,y+23,QuantumUiTheme.BORDER_DIM);

        QuantumUiTheme.panel(gui,x+8,y+28,x+151,y+130);
        QuantumUiTheme.panel(gui,x+154,y+24,x+246,y+130);
        gui.text(font,Component.literal("AREA CONFIGURATION"),x+14,y+34,QuantumUiTheme.MUTED,false);
        gui.text(font,Component.literal("SIZE X      SIZE Y      SIZE Z"),x+14,y+54,QuantumUiTheme.TEXT_SOFT,false);
        gui.text(font,Component.literal("OFFSET X    OFFSET Y    OFFSET Z"),x+14,y+97,QuantumUiTheme.TEXT_SOFT,false);

        for (int i=0;i<6;i++) {
            EditBox f=fields[i];
            if(f!=null) QuantumUiTheme.panel(gui,f.getX()-2,f.getY()-2,f.getX()+f.getWidth()+2,f.getY()+f.getHeight()+2,QuantumUiTheme.BORDER_DIM,QuantumUiTheme.DEEP);
        }
        drawEnergy(gui,x,y);
        drawProgress(gui,x,y);
        QuantumUiTheme.slotFrame(gui,x+84,y+42,menu.builder()!=null&&menu.builder().hasShapeCard(),QuantumUiTheme.CYAN);
        QuantumUiTheme.slotFrame(gui,x+120,y+42,menu.builder()!=null&&menu.builder().hasQuarryCard(),QuantumUiTheme.AMBER);

        gui.fill(x+8,y+137,x+imageWidth-8,y+138,QuantumUiTheme.BORDER_DIM);
        gui.text(font,Component.literal("PLAYER INVENTORY"),x+47,y+139,QuantumUiTheme.MUTED,false);
        if(!validation.isBlank()) gui.text(font,Component.literal(validation),x+158,y+118,QuantumUiTheme.RED,false);
    }

    private void drawEnergy(GuiGraphicsExtractor gui,int x,int y){
        int energy=Math.max(0,menu.data().get(0)),cap=Math.max(1,menu.data().get(1));
        gui.text(font,Component.literal("ENERGY"),x+160,y+55,QuantumUiTheme.MUTED,false);
        QuantumUiTheme.segmentedBar(gui,x+160,y+68,80,9,energy,cap,QuantumUiTheme.CYAN,10);
        gui.text(font,Component.literal(compact(energy)+" / "+compact(cap)+" FE"),x+160,y+82,QuantumUiTheme.TEXT_SOFT,false);
    }

    private void drawProgress(GuiGraphicsExtractor gui,int x,int y){
        int cursor=Math.max(0,menu.data().get(9)),volume=Math.max(1,menu.data().get(10)),status=menu.data().get(11);
        gui.text(font,Component.literal("PROGRESS"),x+160,y+96,QuantumUiTheme.MUTED,false);
        QuantumUiTheme.segmentedBar(gui,x+160,y+108,80,9,cursor,volume,QuantumUiTheme.GREEN,10);
        gui.text(font,statusText(status),x+160,y+120,statusColor(status),false);
    }

    private static String compact(long v){ if(v>=1_000_000)return String.format("%.1fM",v/1_000_000.0); if(v>=1000)return String.format("%.1fk",v/1000.0); return Long.toString(v); }
    private static Component statusText(int s){return Component.literal(switch(s){case 1->"RUNNING";case 2->"NO CARD";case 3->"NO ENERGY";case 4->"OUTPUT FULL";case 5->"DONE";case 6->"PAUSED";default->"IDLE";});}
    private static int statusColor(int s){return switch(s){case 1->QuantumUiTheme.CYAN;case 5->QuantumUiTheme.GREEN;case 2,3,4,6->QuantumUiTheme.AMBER;default->QuantumUiTheme.TEXT_SOFT;};}
    @Override protected void extractLabels(GuiGraphicsExtractor gui,int mouseX,int mouseY){}
}
