package com.flipadvisor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.PluginPanel;

final class FlipAdvisorPanel extends PluginPanel
{
	private static final NumberFormat NUMBER = NumberFormat.getIntegerInstance(Locale.US);

	private final JPanel candidateList = new JPanel();
	private final JPanel activeOfferList = new JPanel();
	private final JLabel status = new JLabel("Loading market data...");
	private final JLabel selectedItem = new JLabel("No suggestion selected");
	private final JComboBox<RiskLevel> risk = new JComboBox<>(RiskLevel.values());
	private final JComboBox<String> horizon = new JComboBox<>(new String[]{"5m", "30m", "2h", "8h", "Custom"});
	private final JSpinner customMinutes = new JSpinner(new SpinnerNumberModel(30, 5, 1440, 5));
	private final Consumer<FlipOpportunity> onSelect;
	private final Runnable onRefresh;
	private final Runnable onInsertPrice;
	private final Runnable onInsertQuantity;
	private final FilterListener filterListener;

	FlipAdvisorPanel(
		Runnable onRefresh,
		Consumer<FlipOpportunity> onSelect,
		Runnable onInsertPrice,
		Runnable onInsertQuantity,
		FilterListener filterListener)
	{
		super(false);
		this.onRefresh = onRefresh;
		this.onSelect = onSelect;
		this.onInsertPrice = onInsertPrice;
		this.onInsertQuantity = onInsertQuantity;
		this.filterListener = filterListener;

		setBorder(new EmptyBorder(8, 8, 8, 8));
		setLayout(new BorderLayout(0, 8));

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

		JLabel title = new JLabel("Flip Advisor");
		title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
		title.setAlignmentX(LEFT_ALIGNMENT);
		content.add(title);
		content.add(Box.createVerticalStrut(8));

		JPanel controls = new JPanel();
		controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
		controls.setAlignmentX(LEFT_ALIGNMENT);
		controls.setBorder(BorderFactory.createTitledBorder("Scan settings"));
		addControlRow(controls, "Risk", risk);
		addControlRow(controls, "Holding time", horizon);
		addControlRow(controls, "Custom minutes", customMinutes);
		JButton refresh = new JButton("Refresh market data");
		refresh.addActionListener(event -> onRefresh.run());
		refresh.setAlignmentX(LEFT_ALIGNMENT);
		controls.add(refresh);
		content.add(controls);

		horizon.setSelectedItem("30m");
		risk.addActionListener(event -> notifyFiltersChanged());
		horizon.addActionListener(event -> {
			customMinutes.setEnabled("Custom".equals(horizon.getSelectedItem()));
			notifyFiltersChanged();
		});
		customMinutes.addChangeListener(event -> notifyFiltersChanged());
		customMinutes.setEnabled(false);

		status.setAlignmentX(LEFT_ALIGNMENT);
		status.setBorder(new EmptyBorder(8, 2, 8, 2));
		content.add(status);

		JPanel selected = new JPanel();
		selected.setLayout(new BoxLayout(selected, BoxLayout.Y_AXIS));
		selected.setAlignmentX(LEFT_ALIGNMENT);
		selected.setBorder(BorderFactory.createTitledBorder("Selected suggestion"));
		selected.add(selectedItem);
		JButton setPrice = new JButton("Insert suggested price");
		setPrice.setToolTipText("Only fills the currently open numeric input; it does not submit it.");
		setPrice.addActionListener(event -> onInsertPrice.run());
		selected.add(setPrice);
		JButton setQuantity = new JButton("Insert suggested amount");
		setQuantity.setToolTipText("Only fills the currently open numeric input; it does not submit it.");
		setQuantity.addActionListener(event -> onInsertQuantity.run());
		selected.add(setQuantity);
		content.add(selected);

		JPanel activeOffers = new JPanel();
		activeOffers.setLayout(new BoxLayout(activeOffers, BoxLayout.Y_AXIS));
		activeOffers.setAlignmentX(LEFT_ALIGNMENT);
		activeOffers.setBorder(BorderFactory.createTitledBorder("Live GE offer guidance"));
		activeOfferList.setLayout(new BoxLayout(activeOfferList, BoxLayout.Y_AXIS));
		activeOffers.add(activeOfferList);
		content.add(activeOffers);

		JPanel candidates = new JPanel();
		candidates.setLayout(new BoxLayout(candidates, BoxLayout.Y_AXIS));
		candidates.setBorder(BorderFactory.createTitledBorder("Opportunities"));
		candidateList.setLayout(new BoxLayout(candidateList, BoxLayout.Y_AXIS));
		candidates.add(candidateList);
		content.add(candidates);

		JScrollPane scroll = new JScrollPane(content);
		scroll.setBorder(null);
		scroll.setPreferredSize(new Dimension(230, 600));
		add(scroll, BorderLayout.CENTER);
	}

	void showActiveOffers(List<ActiveOfferAdvice> advice)
	{
		SwingUtilities.invokeLater(() -> {
			activeOfferList.removeAll();
			if (advice.isEmpty())
			{
				activeOfferList.add(new JLabel("No active offers."));
			}
			else
			{
				for (ActiveOfferAdvice offerAdvice : advice)
				{
					JLabel label = new JLabel("<html>" + escape(offerAdvice.text) + "</html>");
					label.setBorder(new EmptyBorder(2, 0, 2, 0));
					activeOfferList.add(label);
				}
			}
			activeOfferList.revalidate();
			activeOfferList.repaint();
		});
	}

	void showCandidates(List<FlipOpportunity> opportunities)
	{
		SwingUtilities.invokeLater(() -> {
			candidateList.removeAll();
			if (opportunities.isEmpty())
			{
				candidateList.add(new JLabel("No candidates meet the current filters."));
			}
			for (FlipOpportunity opportunity : opportunities)
			{
				candidateList.add(createCandidateRow(opportunity));
				candidateList.add(Box.createVerticalStrut(5));
			}
			candidateList.revalidate();
			candidateList.repaint();
		});
	}

	void setStatus(String message)
	{
		SwingUtilities.invokeLater(() -> status.setText(message));
	}

	void setSelected(FlipOpportunity opportunity)
	{
		SwingUtilities.invokeLater(() -> {
			if (opportunity == null)
			{
				selectedItem.setText("No suggestion selected");
			}
			else
			{
				selectedItem.setText("<html>" + escape(opportunity.item.name) + "<br>Buy "
					+ NUMBER.format(opportunity.buyPrice) + " / sell "
					+ NUMBER.format(opportunity.sellPrice) + "<br>Amount "
					+ NUMBER.format(opportunity.quantity) + " · est. "
					+ NUMBER.format(opportunity.estimatedProfit) + " gp</html>");
			}
		});
	}

	private JPanel createCandidateRow(FlipOpportunity opportunity)
	{
		JPanel row = new JPanel(new BorderLayout(2, 2));
		row.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(getForeground().darker()),
			new EmptyBorder(4, 4, 4, 4)));
		JLabel details = new JLabel("<html><b>" + escape(opportunity.item.name) + "</b><br>Buy "
			+ NUMBER.format(opportunity.buyPrice) + " · sell " + NUMBER.format(opportunity.sellPrice)
			+ "<br>Est. profit " + NUMBER.format(opportunity.estimatedProfit) + " gp"
			+ "<br>Net margin " + String.format(Locale.US, "%.2f%%", opportunity.netMarginPercent)
			+ " · 1h volume " + NUMBER.format(opportunity.hourlyVolume)
			+ "<br>vs 1h average " + String.format(Locale.US, "%+.1f%%", opportunity.hourlyDeviationPercent)
			+ "</html>");
		row.add(details, BorderLayout.CENTER);
		JButton use = new JButton("Select");
		use.addActionListener(event -> onSelect.accept(opportunity));
		row.add(use, BorderLayout.SOUTH);
		return row;
	}

	private void addControlRow(JPanel parent, String label, java.awt.Component control)
	{
		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
		row.add(new JLabel(label));
		row.add(control);
		parent.add(row);
	}

	private void notifyFiltersChanged()
	{
		String selectedHorizon = (String) horizon.getSelectedItem();
		int minutes;
		switch (selectedHorizon)
		{
			case "5m":
				minutes = 5;
				break;
			case "30m":
				minutes = 30;
				break;
			case "2h":
				minutes = 120;
				break;
			case "8h":
				minutes = 480;
				break;
			default:
				minutes = (Integer) customMinutes.getValue();
				break;
		}
		filterListener.filtersChanged((RiskLevel) risk.getSelectedItem(), minutes);
	}

	private static String escape(String text)
	{
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	interface FilterListener
	{
		void filtersChanged(RiskLevel risk, int holdingWindowMinutes);
	}
}
