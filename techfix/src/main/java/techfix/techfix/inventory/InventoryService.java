package techfix.techfix.inventory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Service;

@Service
public class InventoryService {

	private final ConcurrentMap<String, InventoryItem> inventory = new ConcurrentHashMap<>();

	public InventoryService() {
		inventory.put("PEC-001", new InventoryItem("PEC-001", "Compressor", "Brastemp", 5, new BigDecimal("899.90")));
		inventory.put("PEC-002", new InventoryItem("PEC-002", "Termostato", "Consul", 12, new BigDecimal("140.00")));
		inventory.put("PEC-003", new InventoryItem("PEC-003", "Resistência", "Electrolux", 8, new BigDecimal("210.50")));
		inventory.put("PEC-004", new InventoryItem("PEC-004", "Painel LCD", "Samsung", 3, new BigDecimal("1250.00")));
		inventory.put("PEC-005", new InventoryItem("PEC-005", "Bomba de Água", "Brastemp", 6, new BigDecimal("320.00")));
	}

	public List<InventoryItem> findAll() {
		return new ArrayList<>(inventory.values());
	}

	public InventoryItem create(String type, String brand, int quantity, BigDecimal price) {
		String id = generateId(type, brand);
		String existingId = findExistingId(type, brand);
		if (existingId != null) {
			InventoryItem existing = inventory.get(existingId);
			BigDecimal itemPrice = price != null ? price : existing.price();
			InventoryItem updated = new InventoryItem(existing.id(), existing.type(), existing.brand(),
					existing.quantity() + quantity, itemPrice);
			inventory.put(existingId, updated);
			return updated;
		}

		BigDecimal sanitizedPrice = price != null ? price : BigDecimal.ZERO;
		InventoryItem item = new InventoryItem(id, type.trim(), brand.trim(), quantity, sanitizedPrice);
		inventory.put(id, item);
		return item;
	}

	public Collection<InventoryItem> snapshot() {
		return new ArrayList<>(inventory.values());
	}

	private String generateId(String type, String brand) {
		String typeCode = type.substring(0, Math.min(3, type.length())).toUpperCase();
		String brandCode = brand.substring(0, Math.min(3, brand.length())).toUpperCase();
		String base = typeCode + "-" + brandCode;

		int maxNum = inventory.values().stream().filter(item -> item.id().startsWith("PEC-" + base + "-")).mapToInt(item -> {
			try {
				String numPart = item.id().substring(item.id().lastIndexOf("-") + 1);
				return Integer.parseInt(numPart);
			} catch (Exception e) {
				return 0;
			}
		}).max().orElse(0);

		return "PEC-" + base + "-" + String.format("%03d", maxNum + 1);
	}

	private String findExistingId(String type, String brand) {
		return inventory.values().stream()
				.filter(item -> item.type().equalsIgnoreCase(type) && item.brand().equalsIgnoreCase(brand))
				.map(InventoryItem::id).findFirst().orElse(null);
	}

	public record InventoryItem(String id, String type, String brand, int quantity, BigDecimal price) {
	}
}

