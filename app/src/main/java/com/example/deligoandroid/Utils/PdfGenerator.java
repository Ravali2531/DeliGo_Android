package com.example.deligoandroid.Utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class PdfGenerator {
    private static final String TAG = "PdfGenerator";

    public interface PdfGenerationCallback {
        void onPdfGenerated(File file);
    }

    public static void generateOrderReceipt(Context context, Map<String, Object> order, PdfGenerationCallback callback) {
        // First get the restaurant name from Firebase
        String orderId = (String) order.get("orderId");
        String restaurantId = (String) order.get("restaurantId");
        
        DatabaseReference restaurantRef = FirebaseDatabase.getInstance()
                .getReference("restaurants")
                .child(restaurantId)
                .child("store_info")
                .child("name");

        restaurantRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String restaurantName = task.getResult().getValue(String.class);
                generatePdfWithRestaurantName(context, order, restaurantName, callback);
            } else {
                generatePdfWithRestaurantName(context, order, "restaurant ravali", callback);
            }
        });
    }

    private static void generatePdfWithRestaurantName(Context context, Map<String, Object> order, 
                                                    String restaurantName, PdfGenerationCallback callback) {
        try {
            // Create directory for receipts in public Downloads folder
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File dir = new File(downloadsDir, "DeliGo_Receipts");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Create file for the receipt
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "Receipt_" + timeStamp + ".pdf";
            File file = new File(dir, fileName);

            // Initialize PDF writer
            PdfWriter writer = new PdfWriter(new FileOutputStream(file));
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Add header
            Paragraph header = new Paragraph("DeliGo - Order Receipt")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(20);
            document.add(header);

            // Add order details
            document.add(new Paragraph("Order ID: " + order.get("orderId")));
            document.add(new Paragraph("Date: " + formatDate((Long) order.get("timestamp"))));
            document.add(new Paragraph("Restaurant: " + restaurantName));
            document.add(new Paragraph(""));

            // Create items table
            Table table = new Table(UnitValue.createPercentArray(new float[]{15, 45, 20, 20}));
            table.setWidth(UnitValue.createPercentValue(100));

            // Add table headers
            table.addHeaderCell(new Cell().add(new Paragraph("Qty")));
            table.addHeaderCell(new Cell().add(new Paragraph("Item")));
            table.addHeaderCell(new Cell().add(new Paragraph("Price")));
            table.addHeaderCell(new Cell().add(new Paragraph("Total")));

            // Add items
            Object itemsObj = order.get("items");
            double subtotal = 0;
            
            if (itemsObj != null) {
                List<Map<String, Object>> itemsList = new ArrayList<>();
                
                // Handle both ArrayList and Map cases
                if (itemsObj instanceof List) {
                    itemsList = (List<Map<String, Object>>) itemsObj;
                } else if (itemsObj instanceof Map) {
                    Map<String, Object> itemsMap = (Map<String, Object>) itemsObj;
                    for (Object value : itemsMap.values()) {
                        if (value instanceof Map) {
                            itemsList.add((Map<String, Object>) value);
                        }
                    }
                } else {
                    Log.e(TAG, "Items must be either a List or Map");
                    throw new IllegalArgumentException("Items must be either a List or Map");
                }

                for (Map<String, Object> item : itemsList) {
                    try {
                        int quantity = ((Number) item.get("quantity")).intValue();
                        double price = ((Number) item.get("price")).doubleValue();
                        double total = quantity * price;

                        table.addCell(new Cell().add(new Paragraph(String.valueOf(quantity))));
                        table.addCell(new Cell().add(new Paragraph((String) item.get("name"))));
                        table.addCell(new Cell().add(new Paragraph(String.format("$%.2f", price))));
                        table.addCell(new Cell().add(new Paragraph(String.format("$%.2f", total))));

                        // Add customizations if any
                        Object customizationsObj = item.get("customizations");
                        if (customizationsObj instanceof Map) {
                            Map<String, Object> customizations = (Map<String, Object>) customizationsObj;
                            for (Map.Entry<String, Object> category : customizations.entrySet()) {
                                try {
                                    Object categoryValue = category.getValue();
                                    Map<String, Object> categoryData;
                                    
                                    if (categoryValue instanceof Map) {
                                        categoryData = (Map<String, Object>) categoryValue;
                                    } else if (categoryValue instanceof List && !((List<?>) categoryValue).isEmpty()) {
                                        categoryData = (Map<String, Object>) ((List<?>) categoryValue).get(0);
                                    } else {
                                        continue;
                                    }
                                    
                                    Object selectionsObj = categoryData.get("selections");
                                    if (selectionsObj instanceof Map) {
                                        Map<String, Object> selections = (Map<String, Object>) selectionsObj;
                                        for (Object selectionObj : selections.values()) {
                                            if (selectionObj instanceof Map) {
                                                Map<String, Object> selection = (Map<String, Object>) selectionObj;
                                                String optionName = (String) selection.get("name");
                                                Object priceObj = selection.get("price");
                                                double optionPrice = priceObj instanceof Number ? 
                                                    ((Number) priceObj).doubleValue() : 0.0;

                                                table.addCell(new Cell().add(new Paragraph("")));
                                                table.addCell(new Cell().add(new Paragraph("+ " + optionName)));
                                                table.addCell(new Cell().add(new Paragraph(String.format("$%.2f", optionPrice))));
                                                table.addCell(new Cell().add(new Paragraph(String.format("$%.2f", optionPrice))));
                                                
                                                total += optionPrice;
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error processing customization: " + e.getMessage());
                                }
                            }
                        }
                        subtotal += total;
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing item: " + e.getMessage());
                    }
                }
            }

            document.add(table);

            // Add totals
            document.add(new Paragraph(""));
            document.add(new Paragraph("Subtotal: $" + String.format("%.2f", subtotal))
                    .setTextAlignment(TextAlignment.RIGHT));
            
            // Add delivery fee with null check
            Object deliveryFeeObj = order.get("deliveryFee");
            double deliveryFee = 0.0;
            if (deliveryFeeObj instanceof Number) {
                deliveryFee = ((Number) deliveryFeeObj).doubleValue();
            }
            document.add(new Paragraph("Delivery Fee: $" + String.format("%.2f", deliveryFee))
                    .setTextAlignment(TextAlignment.RIGHT));

            // Add tip with null check
            Object tipAmountObj = order.get("tipAmount");
            double tipAmount = 0.0;
            if (tipAmountObj instanceof Number) {
                tipAmount = ((Number) tipAmountObj).doubleValue();
                if (tipAmount > 0) {
                    document.add(new Paragraph("Tip: $" + String.format("%.2f", tipAmount))
                            .setTextAlignment(TextAlignment.RIGHT));
                }
            }

            // Add final total with null check
            Object totalObj = order.get("total");
            double finalTotal = subtotal + deliveryFee + tipAmount; // Calculate total if not provided
            if (totalObj instanceof Number) {
                finalTotal = ((Number) totalObj).doubleValue();
            }
            document.add(new Paragraph("Total: $" + String.format("%.2f", finalTotal))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBold());

            // Add footer
            document.add(new Paragraph(""));
            document.add(new Paragraph("Thank you for ordering with DeliGo!")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setItalic());

            document.close();
            callback.onPdfGenerated(file);

        } catch (Exception e) {
            Log.e(TAG, "Error generating PDF: " + e.getMessage());
            callback.onPdfGenerated(null);
        }
    }

    private static String formatDate(Long timestamp) {
        if (timestamp == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
} 