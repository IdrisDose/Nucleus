/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfigAdapter;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.HoverAction;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyle;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

public class ChatUtil {

    private final NucleusPlugin plugin;
    private final Pattern urlParser =
        Pattern.compile("(?<first>(^|\\s))(?<colour>(&[0-9a-flmnork])+)?(?<url>(http(s)?://)?([A-Za-z0-9]+\\.)+[A-Za-z0-9]{2,}\\S*)",
        Pattern.CASE_INSENSITIVE);

    private final Pattern tokenParser = Pattern.compile("^\\{\\{(?<capture>[a-zA-Z:]+)\\}\\}", Pattern.CASE_INSENSITIVE);
    private final Pattern tokenParserLookAhead = Pattern.compile("(?=\\{\\{(?<capture>[a-zA-Z:]+)\\}\\})", Pattern.CASE_INSENSITIVE);

    private final Pattern enhancedUrlParser =
            Pattern.compile("(?<first>(^|\\s))(?<colour>(&[0-9a-flmnork])+)?"
                + "((?<url>(http(s)?://)?([A-Za-z0-9]+\\.)+[A-Za-z0-9]{2,}\\S*)|"
                + "(?<specialUrl>(\\[(?<msg>.+)\\]\\((?<sUrl>(http(s)?://)?([A-Za-z0-9]+\\.)+[A-Za-z0-9]{2,}[^\\s)]*)\\)))|"
                + "(?<specialCmd>(\\[(?<sMsg>.+)\\]\\((?<sCmd>/.+)\\))))",
                Pattern.CASE_INSENSITIVE);

    private CoreConfigAdapter cca = null;

    public static final StyleTuple EMPTY = new StyleTuple(TextColors.NONE, TextStyles.NONE);

    public ChatUtil(NucleusPlugin plugin) {
        this.plugin = plugin;
    }

    @SafeVarargs
    @Deprecated
    public final List<Text> getMessageFromTemplate(List<String> templates, CommandSource cs, final boolean trimTrailingSpace,
        Map<String, Function<CommandSource, Optional<Text>>>... tokensArray) {

        if (tokensArray.length == 0) {
            return getMessageFromTemplate(templates, cs, trimTrailingSpace, Maps.newHashMap());
        } else if (tokensArray.length == 1) {
            return getMessageFromTemplate(templates, cs, trimTrailingSpace, tokensArray[0]);
        } else {
            return getMessageFromTemplate(templates, cs, trimTrailingSpace, new HashMap<String, Function<CommandSource, Optional<Text>>>() {{
                Arrays.stream(tokensArray).forEach(this::putAll);
            }});
        }
    }

    public final Text getMessageFromTemplate(String templates, CommandSource cs, final boolean trimTrailingSpace) {
        return getMessageFromTemplate(Lists.newArrayList(templates), cs, trimTrailingSpace, Maps.newHashMap()).get(0);
    }

    public final List<Text> getMessageFromTemplate(List<String> templates, CommandSource cs, final boolean trimTrailingSpace) {
        return getMessageFromTemplate(templates, cs, trimTrailingSpace, Maps.newHashMap());
    }

    public final Text getMessageFromTemplate(String templates, CommandSource cs, final boolean trimTrailingSpace,
        Map<String, Function<CommandSource, Optional<Text>>> tokensArray) {
        return getMessageFromTemplate(Lists.newArrayList(templates), cs, trimTrailingSpace, tokensArray).get(0);
    }

    public final List<Text> getMessageFromTemplate(List<String> templates, CommandSource cs, final boolean trimTrailingSpace,
            Map<String, Function<CommandSource, Optional<Text>>> tokensArray) {

        List<Text> texts = Lists.newArrayList();
        templates.forEach(template -> {
            StyleTuple st = new StyleTuple(TextColors.WHITE, TextStyles.NONE);
            boolean trimNext = trimTrailingSpace;

            Text.Builder tb = Text.builder();

            String[] split = tokenParserLookAhead.split(template);
            if (split.length == 1) {
                texts.add(TextSerializers.FORMATTING_CODE.deserialize(template));
                return;
            }

            String[] items = tokenParserLookAhead.split(template);
            Matcher tokenCheck = tokenParser.matcher("");
            for (String textElement : items) {
                if (tokenCheck.reset(textElement).find(0)) {

                    // You might be wondering what this next line is all about. I'd imagine you'd gather that the replaceAll section
                    // does exactly what you think it does, which is remove the token from the text element. And you'd be right to think that.
                    // However, you may be wondering why I've also put a replace within the group. It turns out that in order to account for
                    // the "intricacies" of Regex where normally {x,y} indicates a repetition, it turns out that the engine doesn't look for
                    // that pattern, but instead just cares that we've started with a "{". So, we need to escape that {, so \{ should work right?
                    //
                    // If you thought it should work, you forgot you're using Java and that nothing is as simple as it seems.
                    //
                    // It turns out that this sadly isn't the case. We actually want the literal "\{", not an escaped "{", so we need to escape
                    // the escape character - so we need to do "\\{". We then stick that into our replaceAll and, just like that, it works!
                    //
                    // Hooray! Time to remove some characters from the beginning of the string.
                    textElement = textElement.replaceAll(tokenCheck.group().replace("{", "\\{"), "");
                    String tokenName = tokenCheck.group("capture");

                    // Token processing here.
                    Optional<Text> tokenResult;
                    if (tokensArray.containsKey(tokenName.toLowerCase())) {
                        tokenResult = tokensArray.get(tokenName.toLowerCase()).apply(cs);
                    } else {
                        tokenResult = plugin.getTokenHandler().getTextFromToken(tokenName, cs);
                    }

                    if (tokenResult.isPresent()) {
                        tb.append(Text.builder().color(st.colour).style(st.style).append(tokenResult.get()).build());
                    } else {
                        tb.append(Text.EMPTY);
                    }

                    trimNext = false;
                }

                if (trimNext) {
                    textElement = textElement.replaceAll("^\\s+", "");
                }

                if (!textElement.isEmpty()) {
                    // Just convert the colour codes, but that's it.
                    Text r = TextSerializers.FORMATTING_CODE.deserialize(textElement);
                    tb.append(Text.of(st.colour, st.style, r));
                    st = getLastColourAndStyle(r, st);
                    trimNext = false;
                }
            }

            texts.add(tb.build());
        });

        return texts;
    }

    public Text addLinksToText(Text message, @Nullable Player player) {
        return addLinksToAmpersandFormattedString(TextSerializers.FORMATTING_CODE.serialize(message), player, enhancedUrlParser);
    }

    public Text addUrlsToAmpersandFormattedString(String message) {
        return addLinksToAmpersandFormattedString(message, null, urlParser);
    }

    public Text addLinksToAmpersandFormattedString(String message, @Nullable Player player, Pattern parser) {
        Preconditions.checkNotNull(message, "message");
        if (message.isEmpty()) {
            return Text.EMPTY;
        }

        Matcher m = parser.matcher(message);
        if (!m.find()) {
            return TextSerializers.FORMATTING_CODE.deserialize(message);
        }

        List<Text> texts = Lists.newArrayList();
        String remaining = message;
        StyleTuple st = ChatUtil.EMPTY;
        do {
            // We found a URL. We split on the URL that we have.
            String[] textArray = remaining.split(parser.pattern(), 2);
            Text first = Text.builder().color(st.colour).style(st.style)
                    .append(TextSerializers.FORMATTING_CODE.deserialize(textArray[0])).build();

            // Add this text to the list regardless.
            texts.add(first);

            // If we have more to do, shove it into the "remaining" variable.
            if (textArray.length == 2) {
                remaining = textArray[1];
            } else {
                remaining = null;
            }

            // Get the last colour & styles
            String colourMatch = m.group("colour");
            if (colourMatch != null && !colourMatch.isEmpty()) {
                first = TextSerializers.FORMATTING_CODE.deserialize(m.group("colour") + " ");
            }

            st = getLastColourAndStyle(first, st);

            // Build the URL
            String whiteSpace = m.group("first");
            if (m.group("url") != null) {
                String url = m.group("url");
                texts.add(getTextForUrl(url, url, whiteSpace, st));
            } else if (m.group("specialUrl") != null) {
                String url = m.group("sUrl");
                String msg = m.group("msg");
                texts.add(getTextForUrl(url, msg, whiteSpace, st));
            } else {
                // Must be rules.
                String cmd = m.group("sCmd");
                String msg = m.group("sMsg");
                if (player != null) {
                    cmd = cmd.replace("{{player}}", player.getName());
                }

                return Text.builder(msg).color(st.colour).style(st.style)
                    .onHover(TextActions.showText(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("chat.command.click", cmd)))
                    .onClick(TextActions.runCommand(cmd))
                    .build();
            }
        } while (remaining != null && m.find());

        // Add the last bit.
        if (remaining != null) {
            texts.add(Text.builder().color(st.colour).style(st.style)
                    .append(TextSerializers.FORMATTING_CODE.deserialize(remaining)).build());
        }

        // Join it all together.
        return Text.join(texts);
    }

    private Text getTextForUrl(String url, String msg, String whiteSpace, StyleTuple st) {
        String toParse = TextSerializers.FORMATTING_CODE.stripCodes(url);
        if (!whiteSpace.isEmpty()) {
            msg = String.join("", whiteSpace, msg);
        }

        try {
            URL urlObj;
            if (!toParse.startsWith("http://") && !toParse.startsWith("https://")) {
                urlObj = new URL("http://" + toParse);
            } else {
                urlObj = new URL(toParse);
            }

            return Text.builder(msg).color(st.colour).style(st.style)
                .onHover(TextActions.showText(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("chat.url.click", url)))
                .onClick(TextActions.openUrl(urlObj))
                .build();
        } catch (MalformedURLException e) {
            // URL parsing failed, just put the original text in here.
            initCoreConfigAdapter();

            plugin.getLogger().warn(plugin.getMessageProvider().getMessageWithFormat("chat.url.malformed", url));
            if (this.cca.getNodeOrDefault().isDebugmode()) {
                e.printStackTrace();
            }

            return Text.builder(url).color(st.colour).style(st.style).build();
        }
    }


    public StyleTuple getLastColourAndStyle(Text text, @Nullable StyleTuple current) {
        List<Text> texts = flatten(text);
        TextColor tc = TextColors.NONE;
        TextStyle ts = TextStyles.NONE;
        for (int i = texts.size() - 1; i > -1; i--) {
            // If we have both a Text Colour and a Text Style, then break out.
            if (tc != TextColors.NONE && ts != TextStyles.NONE) {
                break;
            }
            
            if (tc == TextColors.NONE) {
                tc = texts.get(i).getColor();
            }

            if (ts == TextStyles.NONE) {
                ts = texts.get(i).getStyle();
            }
        }

        if (current == null) {
            return new StyleTuple(tc, ts);
        }

        return new StyleTuple(tc != TextColors.NONE ? tc : current.colour, ts != TextStyles.NONE ? ts : current.style);
    }

    private List<Text> flatten(Text text) {
        List<Text> texts = Lists.newArrayList(text);
        if (!text.getChildren().isEmpty()) {
            text.getChildren().forEach(x -> texts.addAll(flatten(x)));
        }

        return texts;
    }

    public Text addCommandToName(CommandSource p) {
        Text.Builder text = Text.builder(p.getName());
        if (p instanceof User) {
            return addCommandToNameInternal(text, (User)p);
        }

        return text.build();
    }

    public Text addCommandToDisplayName(CommandSource p) {
        Text name = getName(p);
        if (p instanceof User) {
            return addCommandToNameInternal(name, (User)p);
        }

        return name;
    }

    private Text addCommandToNameInternal(Text name, User user) {
        return addCommandToNameInternal(name.toBuilder(), user);
    }

    private Text addCommandToNameInternal(Text.Builder name, User user) {
        initCoreConfigAdapter();
        String cmd = cca.getNodeOrDefault().getCommandOnNameClick();
        if (cmd == null || cmd.isEmpty()) {
            return name.build();
        }

        if (!cmd.startsWith("/")) {
            cmd = "/" + cmd;
        }

        if (!cmd.endsWith(" ")) {
            cmd = cmd + " ";
        }

        final String commandToRun = cmd.replaceAll("\\{\\{player\\}\\}", user.getName());
        Optional<HoverAction<?>> ha = name.getHoverAction();
        Text.Builder hoverAction;
        if (ha.isPresent() && (ha.get() instanceof HoverAction.ShowText)) {
            HoverAction.ShowText h = (HoverAction.ShowText)ha.get();
            hoverAction = h.getResult().toBuilder();
            hoverAction.append(Text.NEW_LINE);
        } else {
            hoverAction = Text.builder();
        }

        hoverAction.append(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("name.hover.command", commandToRun));
        return name.onClick(TextActions.suggestCommand(commandToRun)).onHover(TextActions.showText(hoverAction.toText())).build();
    }

    private Text getName(CommandSource cs) {
        if (cs instanceof Player) {
            return plugin.getNameUtil().getName((Player)cs);
        }

        return Text.of(cs.getName());
    }

    private void initCoreConfigAdapter() {
        if (this.cca == null) {
            this.cca = plugin.getInjector().getInstance(CoreConfigAdapter.class);
        }
    }

    public static final class StyleTuple {
        public final TextColor colour;
        public final TextStyle style;

        StyleTuple(TextColor colour, TextStyle style) {
            this.colour = colour;
            this.style = style;
        }
    }
}
