package mcjty.rftoolsbuilder.client;

import mcjty.rftoolsbuilder.FilterTagPayload;
import mcjty.rftoolsbuilder.QuarryCardItem;
import mcjty.rftoolsbuilder.QuarryFilterMenu;
import mcjty.rftoolsbuilder.constructor.client.ui.QuantumButton;
import mcjty.rftoolsbuilder.constructor.client.ui.QuantumUiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Quarry whitelist/blacklist editor with tag and item-rule controls. */
public final class QuarryFilterScreen extends AbstractContainerScreen<QuarryFilterMenu> {
    private static final int VISIBLE_ROWS = 7;
    private final QuantumButton[] entryButtons = new QuantumButton[VISIBLE_ROWS];
    private EditBox tagField;
    private QuantumButton damageButton, nbtButton, modButton, ruleButton, removeButton, expandButton;
    private int firstVisible;
    private int selected = -1;

    public QuarryFilterScreen(QuarryFilterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 256, 304);
        this.inventoryLabelY = 204;
        this.titleLabelY = 7;
    }

    @Override
    protected void init() {
        super.init();
        int x=leftPos,y=topPos;
        damageButton=addRenderableWidget(new QuantumButton(x+8,y+24,74,18,Component.literal("DAMAGE"),()->send(1),()->QuarryCardItem.damageMode(card()),QuantumUiTheme.CYAN));
        nbtButton=addRenderableWidget(new QuantumButton(x+84,y+24,50,18,Component.literal("NBT"),()->send(2),()->QuarryCardItem.nbtMode(card()),QuantumUiTheme.CYAN));
        modButton=addRenderableWidget(new QuantumButton(x+136,y+24,54,18,Component.literal("MOD"),()->send(3),()->QuarryCardItem.modMode(card()),QuantumUiTheme.CYAN));
        addRenderableWidget(new QuantumButton(x+192,y+24,56,18,Component.literal("CLEAR"),()->send(4),()->false,QuantumUiTheme.RED));

        tagField=new EditBox(font,x+8,y+48,180,16,Component.literal("Block or item tag"));
        tagField.setBordered(false); tagField.setMaxLength(128); tagField.setTextColor(QuantumUiTheme.TEXT); tagField.setHint(Component.literal("namespace:tag"));
        addRenderableWidget(tagField);
        addRenderableWidget(new QuantumButton(x+190,y+48,58,16,Component.literal("ADD TAG"),this::addTag,()->false,QuantumUiTheme.GREEN));

        for(int row=0;row<VISIBLE_ROWS;row++){
            final int r=row;
            entryButtons[row]=addRenderableWidget(new QuantumButton(x+8,y+74+row*17,180,15,Component.empty(),()->selectRow(r),()->firstVisible+r==selected,QuantumUiTheme.CYAN));
        }
        addRenderableWidget(new QuantumButton(x+190,y+74,28,18,Component.literal("▲"),()->scroll(-1)));
        addRenderableWidget(new QuantumButton(x+220,y+74,28,18,Component.literal("▼"),()->scroll(1)));
        ruleButton=addRenderableWidget(new QuantumButton(x+190,y+98,58,18,Component.literal("RULE"),this::toggleSelectedRule,()->selected>=0&&QuarryCardItem.entryBlacklist(card(),selected),QuantumUiTheme.AMBER));
        removeButton=addRenderableWidget(new QuantumButton(x+190,y+122,58,18,Component.literal("REMOVE"),()->{if(selected>=0)send(QuarryFilterMenu.REMOVE_BASE+selected);},()->false,QuantumUiTheme.RED));
        expandButton=addRenderableWidget(new QuantumButton(x+190,y+146,58,18,Component.literal("TO TAGS"),()->{if(selected>=0)send(QuarryFilterMenu.EXPAND_BASE+selected);},()->false,QuantumUiTheme.CYAN));
        syncButtons();
    }

    private void addTag(){
        String tag=tagField.getValue().trim();
        if(tag.isBlank())return;
        ClientPacketDistributor.sendToServer(new FilterTagPayload(menu.cardSlot(),tag));
        tagField.setValue("");
    }
    private void toggleSelectedRule(){if(selected>=0)send(QuarryFilterMenu.TOGGLE_RULE_BASE+selected);}
    private void send(int id){Minecraft mc=minecraft;if(mc!=null&&mc.gameMode!=null)mc.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    private ItemStack card(){return menu.cardStack();}
    private void selectRow(int row){int idx=firstVisible+row;if(idx>=0&&idx<QuarryCardItem.entryCount(card()))selected=idx;syncButtons();}
    private void scroll(int delta){int max=Math.max(0,QuarryCardItem.entryCount(card())-VISIBLE_ROWS);firstVisible=Math.max(0,Math.min(max,firstVisible+delta));syncButtons();}

    private String entryLabel(int index){
        ItemStack card=card(); int tags=QuarryCardItem.tagCount(card);
        String prefix=QuarryCardItem.entryBlacklist(card,index)?"[-] ":"[+] ";
        if(index<tags){String s=QuarryCardItem.getTag(card,index);return prefix+"#"+trim(s,25);}
        if(minecraft==null||minecraft.player==null)return prefix+"item";
        ItemStack item=QuarryCardItem.getFilterItem(card,index-tags,minecraft.player.registryAccess());
        return prefix+(item.isEmpty()?"<invalid item>":trim(item.getHoverName().getString(),25));
    }

    private void syncButtons(){
        int count=QuarryCardItem.entryCount(card());
        if(selected>=count)selected=count-1;
        for(int r=0;r<VISIBLE_ROWS;r++){
            int idx=firstVisible+r; QuantumButton b=entryButtons[r]; if(b==null)continue;
            b.visible=idx<count; b.active=idx<count; if(idx<count)b.setMessage(Component.literal(entryLabel(idx)));
        }
        boolean selectedValid=selected>=0&&selected<count;
        if(ruleButton!=null){ruleButton.active=selectedValid;ruleButton.setMessage(Component.literal(selectedValid&&QuarryCardItem.entryBlacklist(card(),selected)?"BLACKLIST":"WHITELIST"));}
        if(removeButton!=null)removeButton.active=selectedValid;
        if(expandButton!=null)expandButton.active=selectedValid&&selected>=QuarryCardItem.tagCount(card());
    }

    @Override protected void containerTick(){super.containerTick();syncButtons();}

    @Override
    public void extractBackground(GuiGraphicsExtractor gui,int mouseX,int mouseY,float partialTick){
        int x=leftPos,y=topPos;
        QuantumUiTheme.window(gui,x,y,imageWidth,imageHeight);
        QuantumUiTheme.title(gui,font,Component.literal("QUARRY // FILTER MATRIX"),x+imageWidth/2,y+7);
        gui.fill(x+8,y+21,x+imageWidth-8,y+22,QuantumUiTheme.BORDER_DIM);
        QuantumUiTheme.panel(gui,x+6,y+44,x+250,y+68);
        QuantumUiTheme.panel(gui,x+6,y+70,x+250,y+198);
        QuantumUiTheme.panel(gui,tagField.getX()-2,tagField.getY()-2,tagField.getX()+tagField.getWidth()+2,tagField.getY()+tagField.getHeight()+2,QuantumUiTheme.BORDER_DIM,QuantumUiTheme.DEEP);

        int white=QuarryCardItem.whitelistCount(card()),black=QuarryCardItem.blacklistCount(card());
        gui.text(font,Component.literal("RULES  "+white+" ALLOW  /  "+black+" DENY"),x+10,y+181,QuantumUiTheme.TEXT_SOFT,false);
        gui.text(font,Component.literal("Click an inventory item to add it · Shift-click adds its tags"),x+10,y+192,QuantumUiTheme.MUTED,false);
        gui.fill(x+8,y+202,x+imageWidth-8,y+203,QuantumUiTheme.BORDER_DIM);
        gui.text(font,Component.literal("PLAYER INVENTORY"),x+47,y+204,QuantumUiTheme.MUTED,false);
    }

    private static String trim(String s,int max){return s.length()<=max?s:s.substring(0,Math.max(1,max-1))+"…";}
    @Override protected void extractLabels(GuiGraphicsExtractor gui,int mouseX,int mouseY){}
}
