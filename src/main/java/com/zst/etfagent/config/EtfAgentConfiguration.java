package com.zst.etfagent.config;

import com.agent4j.api.Agent;
import com.agent4j.api.Handoff;
import com.agent4j.core.AgentDefinition;
import com.zst.etfagent.compliance.EtfComplianceGuardrails;
import com.zst.etfagent.tools.EtfSeekToolFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EtfAgentProperties.class)
public class EtfAgentConfiguration {

    @Bean
    public Agent etfResearchAgent(EtfSeekToolFactory tools, EtfComplianceGuardrails guardrails) {
        Agent themeAgent = specialist(
                "ETF Theme Discovery Agent",
                "Map user themes, industries, and market views to ETF SEEK sectors. Use w1055046_search_sectors first and explain sSectcode, sector name, dataSource, and match reason.",
                tools
        );
        Agent indexAgent = specialist(
                "Index Fit Agent",
                "Analyze sector-index fit. Use w1055048_list_indexes_by_sector and focus on sectRelation, sectCoverWeight, sectCoverRate, valuation, and index style.",
                tools
        );
        Agent etfAgent = specialist(
                "ETF Due Diligence Agent",
                "Compare ETFs linked to an index. Use w1055011_list_etfs_by_index and get_etf_detail. Explain scale, fee, tracking error, IOPV premium/discount, turnover, fund company, manager, and setup date.",
                tools
        );
        Agent reportAgent = specialist(
                "Institutional Report Agent",
                "Generate B2B-ready research output. Prefer generate_institutional_insight when the user asks for a complete recommendation memo, sales enablement note, or product shelf comparison.",
                tools
        );

        return new AgentDefinition()
                .setName("ETF SEEK Institutional Research Agent")
                .setInstructions("""
                        You are the ETF SEEK Institutional Research Agent for B2B users: brokers, fund sales teams, investment advisors, wealth managers, and research desks.
                        Your value is not ordinary API calling. You must turn ETF SEEK data into a traceable institutional workflow:
                        keyword -> sector -> index fit -> linked ETFs -> due-diligence scorecard -> risk flags -> B2B research/sales material.
                        Prefer generate_institutional_insight for complete theme-to-ETF tasks, then use lower-level tools only when more detail is needed.
                        Always preserve data lineage: sourceApi, request fields, matched sector, selected index, candidate ETFs, scoring evidence, exclusions, and missing data.
                        Never provide guaranteed return, principal protection, explicit buy/sell instruction, short-term price prediction, or personalized investment advice.
                        Use a professional but commercially useful style: clear conclusion, comparison table, evidence cards, watchlist risks, and advisor-ready talking points.
                        """)
                .setTools(tools.allTools())
                .addHandoff(handoff("transfer_to_theme_agent", "Use when the user only gives a theme, industry, keyword, or broad market view.", themeAgent))
                .addHandoff(handoff("transfer_to_index_agent", "Use when the user asks about index relevance, valuation, trend, constituents, or period returns.", indexAgent))
                .addHandoff(handoff("transfer_to_etf_agent", "Use when the user asks for ETF candidates, product comparison, fees, tracking error, liquidity, or fund company details.", etfAgent))
                .addHandoff(handoff("transfer_to_report_agent", "Use when the user asks for a client-manager script, research brief, product shelf memo, or reusable business material.", reportAgent))
                .addInputGuardrail(guardrails.inputGuardrail())
                .addOutputGuardrail(guardrails.outputGuardrail())
                .build();
    }

    private static Agent specialist(String name, String instructions, EtfSeekToolFactory tools) {
        return new AgentDefinition()
                .setName(name)
                .setInstructions(instructions + "\nAll outputs must be research-assistive, traceable, and non-advisory.")
                .setTools(tools.allTools())
                .build();
    }

    private static Handoff handoff(String toolName, String description, Agent targetAgent) {
        return new Handoff() {
            @Override
            public String getToolName() {
                return toolName;
            }

            @Override
            public String getToolDescription() {
                return description;
            }

            @Override
            public Agent getTargetAgent() {
                return targetAgent;
            }
        };
    }
}
