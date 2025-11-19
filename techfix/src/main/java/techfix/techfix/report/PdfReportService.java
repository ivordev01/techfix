package techfix.techfix.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import techfix.techfix.customer.CustomerService.Customer;
import techfix.techfix.inventory.InventoryService.InventoryItem;
import techfix.techfix.ticket.TicketController.BudgetRequest;
import techfix.techfix.ticket.TicketController.BudgetRequest.BudgetItem;
import techfix.techfix.ticket.TicketController.BudgetRequest.LaborInfo;
import techfix.techfix.ticket.TicketController.LaudoRequest;
import techfix.techfix.ticket.TicketService.Ticket;

@Service
public class PdfReportService {
	private static final float MARGIN = 50f;
	private static final float LINE_HEIGHT = 16f;
	private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

	public byte[] generateBudgetPdf(Ticket ticket, Customer customer, BudgetRequest request) {
		try (PDDocument document = new PDDocument()) {
			PDPage page = new PDPage(PDRectangle.A4);
			document.addPage(page);

			try (PDPageContentStream content = new PDPageContentStream(document, page)) {
				float y = startPage(content);
				y = writeHeading(content, "TechFix · Orçamento sem compromisso", y);
				y = writeParagraph(content, "Emitido em: " + now(), y);
				y = blankLine(content, y);

				y = writeSectionTitle(content, "Dados do cliente", y);
				y = writeParagraph(content, "Cliente: " + customer.name(), y);
				y = writeParagraph(content, "CPF: " + customer.cpf(), y);
				y = writeParagraph(content, "Telefone: " + customer.phone(), y);
				y = writeParagraph(content, "Endereço: " + customer.address(), y);
				y = blankLine(content, y);

				y = writeSectionTitle(content, "Descrição do equipamento", y);
				y = writeParagraph(content, "Chamado: " + ticket.id(), y);
				y = writeParagraph(content, "Equipamento: " + ticket.device(), y);
				y = writeParagraph(content, "Status atual: " + ticket.status().name(), y);
				y = writeParagraph(content, "Queixa do cliente: " + ticket.description(), y);
				y = blankLine(content, y);

				y = writeSectionTitle(content, "Diagnóstico preliminar", y);
				y = writeParagraph(content, "Diagnóstico: " + request.diagnosis(), y);
				y = writeParagraph(content, "Causa provável: " + request.cause(), y);
				y = writeParagraph(content, "Equipamento tem conserto? " + yesNo(request.equipmentHasConsent()), y);
				y = writeParagraph(content, "Fonte de alimentação afetada? " + yesNo(request.powerSupplyAffected()), y);
				y = blankLine(content, y);

				if (request.part() != null) {
					BudgetItem part = request.part();
					y = writeSectionTitle(content, "Peças recomendadas", y);
					y = writeParagraph(content, "Peça: " + part.name(), y);
					y = writeParagraph(content, "Especificação: " + part.specification(), y);
					y = writeParagraph(content, "Código: " + part.code(), y);
					y = writeParagraph(content, "Preço da peça: " + currency(part.partPrice()), y);
					y = writeParagraph(content, "Mão-de-obra sobre peça: " + currency(part.laborPrice()), y);
					y = blankLine(content, y);
				}

				if (request.laborInfo() != null) {
					LaborInfo labor = request.laborInfo();
					y = writeSectionTitle(content, "Serviços", y);
					y = writeParagraph(content, "Serviço: " + labor.description(), y);
					y = writeParagraph(content, "Tempo estimado: " + labor.estimatedTime(), y);
					y = writeParagraph(content, "Responsável: " + labor.responsible(), y);
					y = blankLine(content, y);
				}

				BigDecimal total = total(request);
				y = writeSectionTitle(content, "Resumo financeiro", y);
				y = writeParagraph(content, "Valor estimado total: " + currency(total), y);
				y = blankLine(content, y);
				writeParagraph(content, "Observação: valores estimados sujeitos a confirmação após diagnóstico completo.", y);
			}

			return toBytes(document);
		} catch (IOException ex) {
			throw new IllegalStateException("Falha ao gerar PDF de orçamento", ex);
		}
	}

	public byte[] generateLaudoPdf(Ticket ticket, Customer customer, LaudoRequest request) {
		try (PDDocument document = new PDDocument()) {
			PDPage page = new PDPage(PDRectangle.A4);
			document.addPage(page);

			try (PDPageContentStream content = new PDPageContentStream(document, page)) {
				float y = startPage(content);
				y = writeHeading(content, "TechFix · Laudo Técnico", y);
				y = writeParagraph(content, "Emitido em: " + now(), y);
				y = blankLine(content, y);

				y = writeSectionTitle(content, "Chamado", y);
				y = writeParagraph(content, "Chamado: " + ticket.id(), y);
				y = writeParagraph(content, "Entrada: " + ticket.entryDate(), y);
				y = writeParagraph(content, "Equipamento: " + ticket.device(), y);
				y = writeParagraph(content, "Status: " + ticket.status().name(), y);
				y = blankLine(content, y);

				y = writeSectionTitle(content, "Cliente", y);
				y = writeParagraph(content, "Nome: " + customer.name(), y);
				y = writeParagraph(content, "Telefone: " + customer.phone(), y);
				y = writeParagraph(content, "Endereço: " + customer.address(), y);
				y = blankLine(content, y);

				y = writeSectionTitle(content, "Queixa registrada", y);
				y = writeParagraph(content, ticket.description(), y);
				y = blankLine(content, y);

				y = writeSectionTitle(content, "Análise técnica", y);
				y = writeParagraph(content, "Condições encontradas: " + request.equipmentCondition(), y);
				y = writeParagraph(content, "Problemas reportados: " + request.problemDescription(), y);
				y = writeParagraph(content, "Diagnóstico técnico: " + request.diagnostic(), y);
				y = blankLine(content, y);

				y = writeSectionTitle(content, "Intervenções e recomendações", y);
				y = writeParagraph(content, "Ações realizadas: " + request.actionsTaken(), y);
				y = writeParagraph(content, "Recomendações: " + request.recommendations(), y);
				y = blankLine(content, y);

				writeParagraph(content, "Responsável técnico: " + request.technicianName(), y);
			}

			return toBytes(document);
		} catch (IOException ex) {
			throw new IllegalStateException("Falha ao gerar PDF de laudo", ex);
		}
	}

	public byte[] generateInventoryReport(Collection<InventoryItem> items) {
		try (PDDocument document = new PDDocument()) {
			PDPage page = new PDPage(PDRectangle.A4);
			document.addPage(page);

			try (PDPageContentStream content = new PDPageContentStream(document, page)) {
				float y = startPage(content);
				y = writeHeading(content, "TechFix · Relatório de Estoque", y);
				y = writeParagraph(content, "Emitido em: " + now(), y);
				y = blankLine(content, y);

				long missing = items.stream().filter(item -> item.quantity() <= 0).count();
				y = writeParagraph(content, "Total de itens catalogados: " + items.size(), y);
				y = writeParagraph(content, "Peças em falta: " + missing, y);
				y = blankLine(content, y);

				for (InventoryItem item : items) {
					y = writeSectionTitle(content, item.id() + " · " + item.type(), y);
					y = writeParagraph(content, "Marca: " + item.brand(), y);
					y = writeParagraph(content, "Quantidade disponível: " + item.quantity(), y);
					y = writeParagraph(content, "Preço de referência: " + currency(item.price()), y);
					y = blankLine(content, y);
				}
			}

			return toBytes(document);
		} catch (IOException ex) {
			throw new IllegalStateException("Falha ao gerar relatório de estoque", ex);
		}
	}

	private byte[] toBytes(PDDocument document) throws IOException {
		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			document.save(output);
			return output.toByteArray();
		}
	}

	private float startPage(PDPageContentStream content) {
		return PDRectangle.A4.getHeight() - MARGIN;
	}

	private float writeHeading(PDPageContentStream content, String text, float y) throws IOException {
		return writeLine(content, text, y, PDType1Font.HELVETICA_BOLD, 18f);
	}

	private float writeSectionTitle(PDPageContentStream content, String text, float y) throws IOException {
		return writeLine(content, text, y, PDType1Font.HELVETICA_BOLD, 12f);
	}

	private float writeParagraph(PDPageContentStream content, String text, float y) throws IOException {
		for (String line : wrap(text, 90)) {
			y = writeLine(content, line, y, PDType1Font.HELVETICA, 11f);
		}
		return y;
	}

	private float blankLine(PDPageContentStream content, float y) throws IOException {
		return y - LINE_HEIGHT;
	}

	private float writeLine(PDPageContentStream content, String text, float y, PDType1Font font, float size)
			throws IOException {
		content.beginText();
		content.setFont(font, size);
		content.newLineAtOffset(MARGIN, y);
		content.showText(text);
		content.endText();
		return y - LINE_HEIGHT;
	}

	private List<String> wrap(String text, int max) {
		if (text == null || text.isBlank()) {
			return List.of("-");
		}

		String normalized = text.replace("\r", "");
		String[] paragraphs = normalized.split("\n");
		java.util.ArrayList<String> lines = new java.util.ArrayList<>();

		for (String paragraph : paragraphs) {
			String trimmed = paragraph.trim();
			if (trimmed.isEmpty()) {
				lines.add("");
				continue;
			}
			String[] words = trimmed.split("\\s+");
			StringBuilder line = new StringBuilder();
			for (String word : words) {
				if (line.length() + word.length() + 1 > max) {
					lines.add(line.toString().trim());
					line = new StringBuilder();
				}
				line.append(word).append(' ');
			}
			if (line.length() > 0) {
				lines.add(line.toString().trim());
			}
		}
		return lines;
	}

	private String currency(BigDecimal value) {
		if (value == null) {
			return CURRENCY.format(BigDecimal.ZERO);
		}
		return CURRENCY.format(value);
	}

	private BigDecimal total(BudgetRequest request) {
		BigDecimal part = request.part() != null && request.part().partPrice() != null ? request.part().partPrice()
				: BigDecimal.ZERO;
		BigDecimal labor = request.part() != null && request.part().laborPrice() != null ? request.part().laborPrice()
				: BigDecimal.ZERO;
		return part.add(labor);
	}

	private String yesNo(boolean value) {
		return value ? "Sim" : "Não";
	}

	private String now() {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
	}
}


