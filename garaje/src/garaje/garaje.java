package garaje;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.sql.Timestamp;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

public class garaje {
    // Datos de conexión a la base de datos
    static final String DB_URL = "jdbc:mysql://localhost:3306/garajen";
    static final String USER = "root";
    static final String PASS = "";
    private static DefaultTableModel modelVehiculos;

    public static void main(String[] args) {
        JFrame frame = new JFrame("CONTROL DE GARAJE");
        frame.setSize(700, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        mostrarPanelInicio(frame);
        frame.setVisible(true);
    }

    public static void mostrarPanelInicio(JFrame frame) {
        frame.getContentPane().removeAll();
        frame.setLayout(new BorderLayout());

        // Ajustar el tamaño de la ventana
        frame.setSize(800, 600); // Tamaño de la ventana
        frame.setLocationRelativeTo(null); // Centrar la ventana

        JPanel panelInicio = new JPanel(new BorderLayout());

        JLabel bienvenida = new JLabel("Bienvenido al Garaje :D", SwingConstants.CENTER);
        bienvenida.setFont(new Font("Arial", Font.BOLD, 16));
        bienvenida.setBorder(BorderFactory.createEmptyBorder(50, 40, 40, 40));

        JPanel panelBotones = new JPanel();
        JButton botonTrabajador = new JButton("Ingresar como trabajador");
        JButton botonAdmin = new JButton("Ingresar como administrador");

        panelBotones.add(botonTrabajador);
        panelBotones.add(botonAdmin);

        botonTrabajador.addActionListener(e -> mostrarMenuTrabajador(frame)); // Corregido

        botonAdmin.addActionListener(e -> mostrarMenuAdmin(frame));

        panelInicio.add(bienvenida, BorderLayout.NORTH);
        panelInicio.add(panelBotones, BorderLayout.CENTER);

        frame.add(panelInicio, BorderLayout.CENTER);
        frame.revalidate();
        frame.repaint();
    }

    // Método para mostrar el menú del trabajador
    public static void mostrarMenuTrabajador(JFrame frame) {
        // Declaración del campo teléfono
        JTextField telefonoField = new JTextField(15);  // Asegúrate de que esta línea esté dentro del método correcto

        frame.getContentPane().removeAll();
        frame.setLayout(new BorderLayout());

        // Título
        JLabel mensaje = new JLabel("Menú Trabajador - Control de Vehículos", SwingConstants.CENTER);
        mensaje.setFont(new Font("Arial", Font.BOLD, 20));

        // Campos para ingresar placa, tipo y conductor
        JTextField placaField = new JTextField(8);
        JComboBox<String> tipoComboBox = new JComboBox<>(new String[]{"Auto", "Moto", "Camioneta", "Discapacitados", "Otros"});
        JTextField conductorField = new JTextField(10);
        JButton botonAsignarEspacio = new JButton("Asignar Espacio");

        // Filtro para limitar longitud de placa
        class LimitadorPlaca extends DocumentFilter {
            private int maxLength;

            public LimitadorPlaca(int maxLength) {
                this.maxLength = maxLength;
            }

            public void setMaxLength(int maxLength) {
                this.maxLength = maxLength;
            }

            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if ((fb.getDocument().getLength() + string.length()) <= maxLength) {
                    super.insertString(fb, offset, string.toUpperCase(), attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if ((fb.getDocument().getLength() - length + text.length()) <= maxLength) {
                    super.replace(fb, offset, length, text.toUpperCase(), attrs);
                }
            }
        }

        LimitadorPlaca filtroPlaca = new LimitadorPlaca(7); // Valor inicial
        ((AbstractDocument) placaField.getDocument()).setDocumentFilter(filtroPlaca);

        // Tabla de espacios disponibles
        String[] columnNames = {"Código de Espacio", "Estado"};
        DefaultTableModel modelEspacios = new DefaultTableModel(columnNames, 0);
        JTable espacioTable = new JTable(modelEspacios);
        JScrollPane scrollEspacios = new JScrollPane(espacioTable);
        scrollEspacios.setPreferredSize(new Dimension(350, 150));

        // Actualizar filtro según tipo de vehículo
        tipoComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String tipoSeleccionado = (String) e.getItem();
                cargarEspaciosDisponibles(modelEspacios, tipoSeleccionado);

                // Establecer la longitud máxima de la placa según el tipo de vehículo seleccionado
                switch (tipoSeleccionado) {
                    case "Moto":
                        filtroPlaca.setMaxLength(6);  // Placas para motos tienen 6 caracteres
                        break;
                    case "Discapacitados":
                    case "Otros":
                        filtroPlaca.setMaxLength(7);  // Placas para discapacitados y otros ahora tienen 7 caracteres
                        break;
                    default: // Auto, Camioneta
                        filtroPlaca.setMaxLength(7);  // Placas para autos y camionetas tienen 7 caracteres
                        break;
                }

                placaField.setText(""); // Limpiar el campo de placa
            }
        });

        // Acción para asignar espacio
        botonAsignarEspacio.addActionListener(e -> {
            String placa = placaField.getText().trim();
            String tipo = tipoComboBox.getSelectedItem().toString();
            String conductor = conductorField.getText().trim();
            String telefono = telefonoField.getText().trim();  // Obtener el teléfono ingresado

            int selectedRow = espacioTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(frame, "Debe seleccionar un espacio.");
                return;
            }

            String codigoEspacio = (String) espacioTable.getValueAt(selectedRow, 0);
            String estado = (String) espacioTable.getValueAt(selectedRow, 1);
            if (estado.equalsIgnoreCase("Ocupado")) {
                JOptionPane.showMessageDialog(frame, "Ese espacio ya está ocupado. Seleccione otro.");
                return;
            }

            if (placa.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "La placa no puede estar vacía.");
                return;
            }

            // Aquí puedes agregar la lógica para asignar espacio y guardar el teléfono en la base de datos
            asignarEspacioManual(placa, tipo, conductor, codigoEspacio, telefono, frame);

            // Generar el ticket PDF con la información de asignación
            generarTicket(placa, tipo, conductor, codigoEspacio, telefono);  // Llamada a la función de generar ticket

            // Limpiar los campos después de asignar el espacio
            placaField.setText("");
            conductorField.setText("");
            telefonoField.setText(""); // Limpiar el campo de teléfono

            // Llamada a las funciones para cargar los espacios disponibles y vehículos activos
            cargarEspaciosDisponibles(modelEspacios, tipo);
            cargarVehiculosActivos(modelVehiculos);
        });

        // Tabla de vehículos activos
        String[] columnasVehiculos = {"ID Registro", "Placa", "Espacio", "Fecha Ingreso"};
        DefaultTableModel modelVehiculos = new DefaultTableModel(columnasVehiculos, 0);
        JTable tablaVehiculos = new JTable(modelVehiculos);
        JScrollPane scrollVehiculos = new JScrollPane(tablaVehiculos);
        scrollVehiculos.setPreferredSize(new Dimension(600, 150));

        cargarVehiculosActivos(modelVehiculos);

        JButton botonRegistrarSalida = new JButton("Registrar Salida");
        botonRegistrarSalida.addActionListener(e -> {
            int fila = tablaVehiculos.getSelectedRow();
            if (fila != -1) {
                int idRegistro = (Integer) modelVehiculos.getValueAt(fila, 0);
                Timestamp fechaIngreso = (Timestamp) modelVehiculos.getValueAt(fila, 3);
                Timestamp fechaSalida = new Timestamp(System.currentTimeMillis());

                long duracionMs = fechaSalida.getTime() - fechaIngreso.getTime();
                long minutosTotales = duracionMs / (60 * 1000);
                long horas = minutosTotales / 60;
                long minutos = minutosTotales % 60;
                String duracion = String.format("%02d:%02d:00", horas, minutos);

                try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
                    String queryTipo = "SELECT t.precio_minuto " +
                            "FROM registro r " +
                            "JOIN tipo_vehiculo tv ON r.espacio_id = tv.id_tipo " +
                            "JOIN tarifa t ON tv.id_tipo = t.tipo_id " +
                            "WHERE r.id_registro = ?";
                    PreparedStatement psTipo = conn.prepareStatement(queryTipo);
                    psTipo.setInt(1, idRegistro);
                    ResultSet rsTipo = psTipo.executeQuery();

                    double precioMinuto = 0.0;
                    if (rsTipo.next()) {
                        precioMinuto = rsTipo.getDouble("precio_minuto");
                    }

                    double totalPago = precioMinuto * minutosTotales;

                    String updateRegistro = "UPDATE registro SET fecha_salida = ?, duracion = ?, precio_minuto = ?, total_pago = ? WHERE id_registro = ?";
                    PreparedStatement psUpdate = conn.prepareStatement(updateRegistro);
                    psUpdate.setTimestamp(1, fechaSalida);
                    psUpdate.setString(2, duracion);
                    psUpdate.setDouble(3, precioMinuto);
                    psUpdate.setDouble(4, totalPago);
                    psUpdate.setInt(5, idRegistro);
                    psUpdate.executeUpdate();

                    String updateEspacio = "UPDATE espacio SET ocupado = FALSE WHERE id_espacio = (SELECT espacio_id FROM registro WHERE id_registro = ?)";
                    PreparedStatement psEspacio = conn.prepareStatement(updateEspacio);
                    psEspacio.setInt(1, idRegistro);
                    psEspacio.executeUpdate();

                    String[] opciones = {"Digital", "Físico"};
                    int opcion = JOptionPane.showOptionDialog(frame,
                            "¿Desea generar un comprobante?",
                            "Tipo de Comprobante",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.INFORMATION_MESSAGE,
                            null,
                            opciones,
                            opciones[0]);

                    if (opcion == -1) {
                        JOptionPane.showMessageDialog(frame, "No se seleccionó el tipo de comprobante.");
                        return;
                    }

                    String tipoComprobante = (opcion == 1) ? "Físico" : "Digital";

                    String queryRegistro = "SELECT r.id_registro, r.placa, r.espacio_id, r.fecha_ingreso, r.fecha_salida, r.duracion, r.total_pago, " +
                            "tv.id_tipo, t.precio_minuto, v.nombre_conductor " +
                            "FROM registro r " +
                            "JOIN tipo_vehiculo tv ON r.espacio_id = tv.id_tipo " +
                            "JOIN tarifa t ON tv.id_tipo = t.tipo_id " +
                            "JOIN vehiculo v ON r.placa = v.placa " +
                            "WHERE r.id_registro = ?";
                    PreparedStatement psRegistro = conn.prepareStatement(queryRegistro);
                    psRegistro.setInt(1, idRegistro);
                    ResultSet rsRegistro = psRegistro.executeQuery();

                    if (rsRegistro.next()) {
                        String placa = rsRegistro.getString("placa");
                        int espacioId = rsRegistro.getInt("espacio_id");
                        fechaIngreso = rsRegistro.getTimestamp("fecha_ingreso");
                        fechaSalida = rsRegistro.getTimestamp("fecha_salida");
                        String nombreConductor = rsRegistro.getString("nombre_conductor");

                        emitirComprobante(idRegistro, tipoComprobante);

                        // ✅ INSERTAR EL REGISTRO DEL COMPROBANTE AQUÍ
                        String insertComprobante = "INSERT INTO comprobante (registro_id, tipo, emitido_en) VALUES (?, ?, NOW())";
                        PreparedStatement psComprobante = conn.prepareStatement(insertComprobante);
                        psComprobante.setInt(1, idRegistro);
                        psComprobante.setString(2, tipoComprobante);
                        psComprobante.executeUpdate();

                        String message = String.format("Salida registrada\n\n" +
                                        "ID Registro: %d\n" +
                                        "Placa: %s\n" +
                                        "Espacio ID: %d\n" +
                                        "Fecha de Ingreso: %s\n" +
                                        "Fecha de Salida: %s\n" +
                                        "Duración: %s\n" +
                                        "Precio por Minuto: S/ %.2f\n" +
                                        "Total a Pagar: S/ %.2f\n" +
                                        "Nombre del Conductor: %s",
                                idRegistro, placa, espacioId, fechaIngreso, fechaSalida, duracion,
                                precioMinuto, totalPago, nombreConductor);

                        JOptionPane.showMessageDialog(frame, message);
                        modelVehiculos.removeRow(fila);
                        cargarEspaciosDisponibles(modelEspacios, tipoComboBox.getSelectedItem().toString());
                    }

                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(frame, "Error al registrar salida: " + ex.getMessage());
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Seleccione un vehículo para registrar su salida.");
            }
        });

        JButton botonActualizar = new JButton("Actualizar espacios");
        botonActualizar.addActionListener(e -> cargarTodosLosEspacios(modelEspacios));

        JButton botonTarifa = new JButton("Seleccionar periodo");
        // Acción para seleccionar tarifa
        botonTarifa.addActionListener(e -> {
            // Mostrar ventana para seleccionar tarifa
            seleccionarTarifaVentana(frame);
        });

        JButton botonAtras = new JButton("Atrás");
        botonAtras.addActionListener(e -> mostrarPanelInicio(frame));

        JPanel panelForm = new JPanel(new GridLayout(6, 2));  // Cambiado a 6 filas
        panelForm.add(new JLabel("Placa:"));
        panelForm.add(placaField);
        panelForm.add(new JLabel("Tipo de vehículo:"));
        panelForm.add(tipoComboBox);
        panelForm.add(new JLabel("Conductor (Opcional):"));
        panelForm.add(conductorField);
        panelForm.add(new JLabel("Teléfono:"));  // Nueva línea para teléfono
        panelForm.add(telefonoField);  // Campo de teléfono
        panelForm.add(new JLabel("Espacios Disponibles:"));
        panelForm.add(scrollEspacios);

        JPanel panelSalidas = new JPanel(new BorderLayout());
        panelSalidas.add(new JLabel("Vehículos en el parqueadero:", SwingConstants.CENTER), BorderLayout.NORTH);
        panelSalidas.add(scrollVehiculos, BorderLayout.CENTER);
        panelSalidas.add(botonRegistrarSalida, BorderLayout.SOUTH);

        JPanel panelBotones = new JPanel();
        panelBotones.add(botonAsignarEspacio);
        panelBotones.add(botonActualizar);
        panelBotones.add(botonTarifa);
        panelBotones.add(botonAtras);

        JPanel panelMenu = new JPanel(new BorderLayout());
        panelMenu.add(mensaje, BorderLayout.NORTH);
        panelMenu.add(panelForm, BorderLayout.WEST);
        panelMenu.add(panelSalidas, BorderLayout.CENTER);
        panelMenu.add(panelBotones, BorderLayout.SOUTH);

        frame.add(panelMenu, BorderLayout.CENTER);
        frame.setSize(1150, 600);
        frame.revalidate();
        frame.repaint();
    }
    // Método para emitir un comprobante
    private static void emitirComprobante(int idRegistro, String tipoComprobante) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            String query = "SELECT r.id_registro, r.placa, r.espacio_id, r.fecha_ingreso, r.fecha_salida, r.duracion, r.total_pago, " +
                    "tv.tipo_vehiculo, t.precio_minuto, v.nombre_conductor " +
                    "FROM registro r " +
                    "JOIN vehiculo v ON r.placa = v.placa " +
                    "JOIN tipo_vehiculo tv ON v.tipo_id = tv.id_tipo " +
                    "JOIN tarifa t ON tv.id_tipo = t.tipo_id " +
                    "WHERE r.id_registro = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, idRegistro);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String contenido = String.format(
                        "========= COMPROBANTE %s =========\n" +
                                "ID Registro: %d\n" +
                                "Placa: %s\n" +
                                "Conductor: %s\n" +
                                "Tipo Vehículo: %s\n" +
                                "Espacio: %s\n" +
                                "Fecha Ingreso: %s\n" +
                                "Fecha Salida: %s\n" +
                                "Duración: %s\n" +
                                "Precio x Minuto: S/ %.2f\n" +
                                "Total Pagado: S/ %.2f\n",
                        tipoComprobante.toUpperCase(),
                        rs.getInt("id_registro"),
                        rs.getString("placa"),
                        rs.getString("nombre_conductor"),
                        rs.getString("tipo_vehiculo"),
                        rs.getInt("espacio_id"),
                        rs.getTimestamp("fecha_ingreso"),
                        rs.getTimestamp("fecha_salida"),
                        rs.getString("duracion"),
                        rs.getDouble("precio_minuto"),
                        rs.getDouble("total_pago")
                );

                if (tipoComprobante.equalsIgnoreCase("Digital")) {
                    JOptionPane.showMessageDialog(null, contenido, "Comprobante Digital", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    // Aquí podrías enviar a impresora o guardar en PDF, pero por ahora mostramos igual
                    JOptionPane.showMessageDialog(null, contenido, "Comprobante Físico (simulado)", JOptionPane.INFORMATION_MESSAGE);
                }
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error al generar el comprobante: " + ex.getMessage());
        }
    }
    // Método para asignar un espacio y registrar el vehículo
    private static void asignarEspacioManual(String placa, String tipo, String conductor, String codigoEspacio, String telefono, JFrame frame) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {

            // Buscar id_tipo por nombre
            int tipoId = -1;
            String tipoNombre = tipo.toLowerCase(); // Asegura coincidencia con la base de datos
            String queryTipo = "SELECT id_tipo FROM tipo_vehiculo WHERE tipo_vehiculo = ?";
            PreparedStatement psTipo = conn.prepareStatement(queryTipo);
            psTipo.setString(1, tipoNombre);
            ResultSet rsTipo = psTipo.executeQuery();
            if (rsTipo.next()) {
                tipoId = rsTipo.getInt("id_tipo");
            } else {
                JOptionPane.showMessageDialog(frame, "Tipo de vehículo no encontrado.");
                return;
            }

            // Verificar si el vehículo ya existe
            String consultaVehiculo = "SELECT placa FROM vehiculo WHERE placa = ?";
            PreparedStatement psVerifica = conn.prepareStatement(consultaVehiculo);
            psVerifica.setString(1, placa);
            ResultSet rsVerifica = psVerifica.executeQuery();

            // Si no existe, lo insertamos
            if (!rsVerifica.next()) {
                String insertarVehiculo = "INSERT INTO vehiculo (placa, nombre_conductor, tipo_id, telefono) VALUES (?, ?, ?, ?)";
                PreparedStatement psInsert = conn.prepareStatement(insertarVehiculo);
                psInsert.setString(1, placa);
                psInsert.setString(2, conductor);
                psInsert.setInt(3, tipoId);
                psInsert.setString(4, telefono);  // Agregar el teléfono aquí
                psInsert.executeUpdate();
            } else {
                // Si el vehículo ya existe, actualizamos el teléfono (en caso de que cambie)
                String updateVehiculo = "UPDATE vehiculo SET telefono = ? WHERE placa = ?";
                PreparedStatement psUpdate = conn.prepareStatement(updateVehiculo);
                psUpdate.setString(1, telefono);
                psUpdate.setString(2, placa);
                psUpdate.executeUpdate();
            }

            // Obtener ID del espacio
            String consultaEspacio = "SELECT id_espacio FROM espacio WHERE codigo = ?";
            PreparedStatement psEspacio = conn.prepareStatement(consultaEspacio);
            psEspacio.setString(1, codigoEspacio);
            ResultSet rsEspacio = psEspacio.executeQuery();

            int espacioId = -1;
            if (rsEspacio.next()) {
                espacioId = rsEspacio.getInt("id_espacio");
            } else {
                JOptionPane.showMessageDialog(frame, "Código de espacio no encontrado.");
                return;
            }

            // Insertar registro
            String insertRegistro = "INSERT INTO registro (placa, espacio_id, fecha_ingreso) VALUES (?, ?, NOW())";
            PreparedStatement psRegistro = conn.prepareStatement(insertRegistro);
            psRegistro.setString(1, placa);
            psRegistro.setInt(2, espacioId);
            psRegistro.executeUpdate();

            // Marcar espacio como ocupado
            String updateEspacio = "UPDATE espacio SET ocupado = TRUE WHERE id_espacio = ?";
            PreparedStatement psOcupar = conn.prepareStatement(updateEspacio);
            psOcupar.setInt(1, espacioId);
            psOcupar.executeUpdate();

            JOptionPane.showMessageDialog(frame, "Espacio asignado correctamente.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame, "Error al asignar espacio: " + ex.getMessage());
        }
    }
    private static int obtenerTipoVehiculo(String tipo) {
        switch (tipo.toLowerCase()) {
            case "auto":
                return 1; // Tipo 1 para "auto"
            case "moto":
                return 2; // Tipo 2 para "moto"
            case "camioneta":
                return 3; // Tipo 3 para "camioneta"
            default:
                return -1; // Tipo no válido
        }
    }
    // Método para cargar espacios disponibles
    private static void cargarEspaciosDisponibles(DefaultTableModel model, String tipoFiltro) {
        model.setRowCount(0); // Limpiar tabla

        // Determinar el prefijo para la consulta según el tipo de vehículo seleccionado
        String letraTipo = tipoFiltro.equalsIgnoreCase("Auto") ? "A" :
                tipoFiltro.equalsIgnoreCase("Moto") ? "M" :
                        tipoFiltro.equalsIgnoreCase("Camioneta") ? "C" :
                                tipoFiltro.equalsIgnoreCase("Discapacitados") ? "D" :
                                        tipoFiltro.equalsIgnoreCase("Otros") ? "O" : ""; // Corregido el ternario

        // Conexión a la base de datos
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            // Consulta SQL para filtrar los espacios disponibles según el tipo de vehículo
            String query = "SELECT codigo, ocupado FROM espacio WHERE ocupado = FALSE AND codigo LIKE ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, letraTipo + "%");  // Aplicar el filtro con el prefijo tipo (A%, M%, C%, D%, O%)

            // Ejecutar la consulta
            ResultSet rs = stmt.executeQuery();

            // Agregar los resultados a la tabla
            while (rs.next()) {
                String codigo = rs.getString("codigo");
                model.addRow(new Object[]{codigo, "Libre"});  // Agregar fila con código de espacio y estado "Libre"
            }
        } catch (SQLException ex) {
            ex.printStackTrace();  // Manejo de excepciones en caso de error de conexión o consulta
        }
    }
    // Metodo para cargar todos los espacios
    private static void cargarTodosLosEspacios(DefaultTableModel model) {
        model.setRowCount(0); // Limpiar tabla

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            String query = "SELECT codigo, ocupado FROM espacio ORDER BY codigo";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String codigo = rs.getString("codigo");
                boolean ocupado = rs.getBoolean("ocupado");
                model.addRow(new Object[]{codigo, ocupado ? "Ocupado" : "Libre"});
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    private static void cargarVehiculosActivos(DefaultTableModel model) {
        if (model == null) {
            System.out.println("El modelo de la tabla no está inicializado.");
            return;
        }

        // Limpiar el modelo antes de cargar los nuevos datos
        model.setRowCount(0);

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            // Cambiar 'espacio' por 'espacio_id' en la consulta SQL
            String query = "SELECT v.placa, v.nombre_conductor, r.id_registro, r.espacio_id, r.fecha_ingreso " +
                    "FROM registro r " +
                    "JOIN vehiculo v ON r.placa = v.placa " +  // JOIN entre las tablas vehiculo y registro
                    "WHERE r.fecha_salida IS NULL";
            PreparedStatement ps = conn.prepareStatement(query);
            ResultSet rs = ps.executeQuery();

            // Procesar el resultado
            while (rs.next()) {
                Object[] row = {
                        rs.getInt("id_registro"),
                        rs.getString("placa"),
                        rs.getInt("espacio_id"), // Accedemos a espacio_id
                        rs.getTimestamp("fecha_ingreso"),
                        rs.getString("nombre_conductor")  // Agregar el nombre del conductor
                };
                model.addRow(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static boolean validarPlacaPorTipo(String placa, String tipo) {
        placa = placa.toUpperCase();

        switch (tipo) {
            case "Auto":
            case "Camioneta":
                return placa.matches("^[A-Z]\\d[A-Z]-\\d{3}$"); // A1B-234
            case "Moto":
                return placa.matches("^[A-Z]{2}-\\d{4}$"); // AB-1234
            case "Discapacitados":
            case "Otros":
                return true; // Sin restricción
            default:
                return false;
        }
    }
    // Función para generar un ticket PDF
    private static void generarTicket(String placa, String tipo, String conductor, String codigoEspacio, String telefono) {
        // Definir el nombre del archivo PDF
        String archivoPDF = "ticket_" + placa + ".pdf";

        try {
            // Crear un documento PDF
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(archivoPDF));

            // Abrir el documento para agregar contenido
            document.open();

            // Título
            document.add(new Paragraph("Ticket de Asignación de Espacio"));
            document.add(new Paragraph("===================================="));
            document.add(new Paragraph("Fecha de emisión: " + new java.util.Date()));
            document.add(new Paragraph("Placa: " + placa));
            document.add(new Paragraph("Tipo de vehículo: " + tipo));
            document.add(new Paragraph("Conductor: " + conductor));
            document.add(new Paragraph("Teléfono: " + telefono));
            document.add(new Paragraph("Espacio asignado: " + codigoEspacio));
            document.add(new Paragraph("===================================="));

            // Cerrar el documento
            document.close();

            // Mostrar mensaje de confirmación
            JOptionPane.showMessageDialog(null, "Ticket generado exitosamente. Ubicación: " + archivoPDF);
        } catch (DocumentException | IOException e) {
            JOptionPane.showMessageDialog(null, "Error al generar el ticket: " + e.getMessage());
        }
    }

    // Función para mostrar la ventana de selección de tarifa (Día/Noche)
    public static void seleccionarTarifaVentana(JFrame frame) {
        frame.getContentPane().removeAll();
        frame.setLayout(new BorderLayout());

        JLabel titulo = new JLabel("Menú Trabajador - Control de Vehículos", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 20));

        JTextField placaField = new JTextField(8);
        JComboBox<String> tipoComboBox = new JComboBox<>(new String[]{"Auto", "Moto", "Camioneta", "Discapacitados", "Otros"});
        JTextField conductorField = new JTextField(10);
        JComboBox<String> tarifaComboBox = new JComboBox<>(new String[]{"Día", "Noche"});
        JTextField montoField = new JTextField(8);
        montoField.setEditable(false);

        // Definir el campo para teléfono
        JTextField telefonoField = new JTextField(10);

        JButton botonAsignarEspacio = new JButton("Asignar Espacio");

        class LimitadorPlaca extends DocumentFilter {
            private int maxLength;

            public LimitadorPlaca(int maxLength) {
                this.maxLength = maxLength;
            }

            public void setMaxLength(int maxLength) {
                this.maxLength = maxLength;
            }

            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if ((fb.getDocument().getLength() + string.length()) <= maxLength) {
                    super.insertString(fb, offset, string.toUpperCase(), attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if ((fb.getDocument().getLength() - length + text.length()) <= maxLength) {
                    super.replace(fb, offset, length, text.toUpperCase(), attrs);
                }
            }
        }

        LimitadorPlaca filtroPlaca = new LimitadorPlaca(7);
        ((AbstractDocument) placaField.getDocument()).setDocumentFilter(filtroPlaca);

        String[] columnasEspacios = {"Código de Espacio", "Estado"};
        DefaultTableModel modelEspacios = new DefaultTableModel(columnasEspacios, 0);
        JTable tablaEspacios = new JTable(modelEspacios);
        JScrollPane scrollEspacios = new JScrollPane(tablaEspacios);
        scrollEspacios.setPreferredSize(new Dimension(300, 120));

        tipoComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String tipoSeleccionado = (String) e.getItem();
                cargarEspaciosDisponibles(modelEspacios, tipoSeleccionado);
                switch (tipoSeleccionado) {
                    case "Moto": filtroPlaca.setMaxLength(6); break;
                    case "Discapacitados":
                    case "Otros": filtroPlaca.setMaxLength(8); break;
                    default: filtroPlaca.setMaxLength(7); break;
                }
                placaField.setText("");
            }
        });

        tarifaComboBox.addActionListener(e -> {
            String seleccion = (String) tarifaComboBox.getSelectedItem();
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
                String sql = "SELECT precio FROM tarifa_selecion WHERE tipo_tarifa = ? ORDER BY fecha_registro DESC LIMIT 1";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, seleccion); // Filtra según el tipo de tarifa (DIA o NOCHE)
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    montoField.setText(String.valueOf(rs.getDouble("precio"))); // Usa "precio" en lugar de "monto"
                } else {
                    montoField.setText("0.00");
                }
            } catch (SQLException ex) {
                montoField.setText("Error");
                ex.printStackTrace(); // Imprime el error si ocurre uno
            }
        });

        tarifaComboBox.setSelectedIndex(0);

        String[] columnasVehiculos = {"ID Registro", "Placa", "Espacio", "Fecha Ingreso"};
        DefaultTableModel modelVehiculos = new DefaultTableModel(columnasVehiculos, 0);
        JTable tablaVehiculos = new JTable(modelVehiculos);
        JScrollPane scrollVehiculos = new JScrollPane(tablaVehiculos);
        scrollVehiculos.setPreferredSize(new Dimension(600, 150));
        cargarVehiculosActivos(modelVehiculos);

        botonAsignarEspacio.addActionListener(e -> {
            String placa = placaField.getText().trim();
            String tipo = tipoComboBox.getSelectedItem().toString();
            String conductor = conductorField.getText().trim();
            String tarifa = tarifaComboBox.getSelectedItem().toString();
            String monto = montoField.getText().trim();
            String telefono = telefonoField.getText().trim();  // Obtener el teléfono ingresado

            int fila = tablaEspacios.getSelectedRow();
            if (fila == -1 || placa.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Complete los campos y seleccione un espacio disponible.");
                return;
            }

            String espacio = (String) modelEspacios.getValueAt(fila, 0);
            asignarEspacioManual(placa, tipo, conductor, espacio, telefono, frame);  // Pasar el teléfono al método

            // Guardar tarifa asociada
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
                String sql = "INSERT INTO tarifas_seleccionadas (id_registro, tipo_tarifa, monto, fecha_registro) VALUES (?, ?, ?, NOW())";
                PreparedStatement ps = conn.prepareStatement(sql);
                int idRegistro = obtenerUltimoIdRegistro(); // ← Debes implementar esto correctamente
                ps.setInt(1, idRegistro);
                ps.setString(2, tarifa);
                ps.setDouble(3, Double.parseDouble(monto));
                ps.executeUpdate();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            placaField.setText("");
            conductorField.setText("");
            telefonoField.setText("");  // Limpiar el campo de teléfono
            cargarEspaciosDisponibles(modelEspacios, tipo);
            cargarVehiculosActivos(modelVehiculos);
        });

        JButton botonActualizarEspacios = new JButton("Actualizar Espacios");
        botonActualizarEspacios.addActionListener(e -> {
            String tipo = tipoComboBox.getSelectedItem().toString();
            cargarEspaciosDisponibles(modelEspacios, tipo);
        });

        JButton botonAtras = new JButton("Atrás");
        botonAtras.addActionListener(e -> mostrarMenuTrabajador(frame));

        JPanel panelForm = new JPanel(new GridLayout(7, 2));  // Asegúrate de tener 7 filas para los nuevos campos
        panelForm.add(new JLabel("Placa:"));
        panelForm.add(placaField);
        panelForm.add(new JLabel("Tipo de Vehículo:"));
        panelForm.add(tipoComboBox);
        panelForm.add(new JLabel("Conductor (opcional):"));
        panelForm.add(conductorField);
        panelForm.add(new JLabel("Tarifa:"));
        panelForm.add(tarifaComboBox);
        panelForm.add(new JLabel("Monto (S/):"));
        panelForm.add(montoField);
        panelForm.add(new JLabel("Teléfono (opcional):"));
        panelForm.add(telefonoField);  // Campo para teléfono
        panelForm.add(new JLabel("Espacios Disponibles:"));
        panelForm.add(scrollEspacios);

        JPanel panelSalidas = new JPanel(new BorderLayout());
        panelSalidas.add(new JLabel("Vehículos en el parqueadero:", SwingConstants.CENTER), BorderLayout.NORTH);
        panelSalidas.add(scrollVehiculos, BorderLayout.CENTER);

        JPanel panelBotones = new JPanel();
        panelBotones.add(botonAsignarEspacio);
        panelBotones.add(botonActualizarEspacios);
        panelBotones.add(botonAtras);

        JPanel panelPrincipal = new JPanel(new BorderLayout());
        panelPrincipal.add(titulo, BorderLayout.NORTH);
        panelPrincipal.add(panelForm, BorderLayout.WEST);
        panelPrincipal.add(panelSalidas, BorderLayout.CENTER);
        panelPrincipal.add(panelBotones, BorderLayout.SOUTH);

        frame.add(panelPrincipal, BorderLayout.CENTER);
        frame.setSize(1150, 600);
        frame.revalidate();
        frame.repaint();
    }
    public static int obtenerUltimoIdRegistro() {
        int id = -1;
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            // Cambiar "garajen" por "registro" y asegurarse de usar la columna correcta
            String sql = "SELECT MAX(id_registro) FROM registro";  // Asegúrate de que la tabla y columna son correctas
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                id = rs.getInt(1);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return id;
    }

    // Menu admin
    public static void mostrarMenuAdmin(JFrame frame) {
        // Solicitar nombre y contraseña al administrador
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Campo para el nombre de usuario
        JTextField nombreField = new JTextField(20);
        nombreField.setMaximumSize(nombreField.getPreferredSize());
        panel.add(new JLabel("Nombre de administrador:"));
        panel.add(nombreField);

        // Campo para la contraseña con validación de longitud
        JPasswordField contrasenaField = new JPasswordField(20);
        contrasenaField.setMaximumSize(contrasenaField.getPreferredSize());
        panel.add(new JLabel("Contraseña:"));
        panel.add(contrasenaField);

        // Mostrar el cuadro de diálogo con los campos
        int option = JOptionPane.showConfirmDialog(frame, panel, "Ingrese las credenciales de administrador", JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            String nombre = nombreField.getText();
            String contrasena = new String(contrasenaField.getPassword());

            // Verificar la longitud de la contraseña (máximo 10 caracteres)
            if (contrasena.length() > 10) {
                JOptionPane.showMessageDialog(frame, "La contraseña no puede tener más de 10 caracteres.");
                return; // Termina la función si la contraseña es demasiado larga
            }

            // Verificar las credenciales en la base de datos
            if (verificarCredencialesAdmin(nombre, contrasena)) {
                // Si la contraseña es correcta, mostrar el menú
                frame.getContentPane().removeAll();
                frame.setLayout(new BorderLayout());

                // Crear componentes del menú de administrador
                JLabel mensaje = new JLabel("Menú Administrador", SwingConstants.CENTER);
                mensaje.setFont(new Font("Arial", Font.BOLD, 20));

                // Botones de acción
                JButton botonGestionVehiculos = new JButton("Gestión de Vehículos");
                JButton botonGenerarReportes = new JButton("Generar Reportes");
                JButton botonReporteTarifa = new JButton("Generar Reportes de las Tarifas");
                JButton botonreportesRegistros = new JButton("Generar Historial de los Registros");
                JButton botonAtras = new JButton("Atrás");

                // Acción para el botón "Gestión de Vehículos"
                botonGestionVehiculos.addActionListener(e -> gestionarVehiculos(frame));

                // Acción para el botón "Generar Reportes"
                botonGenerarReportes.addActionListener(e -> generarReportes(frame));

                // Accion para el boton de Generar reportes de tarifas*
                botonReporteTarifa.addActionListener(e -> reporteTarifa(frame));

                // Accion para el boton de generar reporte de registros
                botonreportesRegistros.addActionListener(e -> reportesRegistros(frame));

                // Acción para el botón "Atrás" que regresa al menú principal
                botonAtras.addActionListener(e -> mostrarPanelInicio(frame));

                // Panel de botones
                JPanel panelBotones = new JPanel();
                panelBotones.add(botonGestionVehiculos);
                panelBotones.add(botonGenerarReportes);
                panelBotones.add(botonReporteTarifa);
                panelBotones.add(botonreportesRegistros);
                panelBotones.add(botonAtras);

                // Panel del menú
                JPanel panelMenu = new JPanel(new BorderLayout());
                panelMenu.add(mensaje, BorderLayout.NORTH);
                panelMenu.add(panelBotones, BorderLayout.CENTER);

                // Agregar el panel del menú a la ventana
                frame.add(panelMenu, BorderLayout.CENTER);
                frame.revalidate();
                frame.repaint();
            } else {
                // Si las credenciales son incorrectas, mostrar mensaje de error
                JOptionPane.showMessageDialog(frame, "Nombre de usuario o contraseña incorrectos. Intente de nuevo.");
            }
        }
    }
    // Método para verificar las credenciales del administrador desde la base de datos
    public static boolean verificarCredencialesAdmin(String nombre, String contrasena) {
        // URL de conexión a la base de datos
        String url = "jdbc:mysql://localhost:3306/garajen";
        String user = "root";
        String password = "";

        // Consulta SQL para verificar el nombre y la contraseña encriptada
        String sql = "SELECT * FROM admin WHERE nombre = ? AND contrasena = ?";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            // Establecer los parámetros en la consulta
            stmt.setString(1, nombre);
            stmt.setString(2, contrasena);

            // Ejecutar la consulta
            ResultSet rs = stmt.executeQuery();

            // Si se encuentra un resultado, las credenciales son correctas
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Método simulado para gestionar vehículos
    public static void gestionarVehiculos(JFrame frame) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            Statement stmt = conn.createStatement();

            // === Tabla de Vehículos ===
            ResultSet rsVehiculos = stmt.executeQuery("SELECT * FROM vehiculo");
            String[] columnasVehiculos = { "Placa", "Tipo de Vehículo", "Conductor", "Teléfono" };
            DefaultTableModel modeloVehiculos = new DefaultTableModel(columnasVehiculos, 0);

            while (rsVehiculos.next()) {
                String placa = rsVehiculos.getString("placa");
                String tipo = rsVehiculos.getString("tipo_id");
                String conductor = rsVehiculos.getString("nombre_conductor");
                String telefono = rsVehiculos.getString("telefono");  // Correcta recuperación de teléfono
                modeloVehiculos.addRow(new Object[]{placa, tipo, conductor, telefono});  // Se añade el teléfono a la fila
            }

            JTable tablaVehiculos = new JTable(modeloVehiculos);
            JScrollPane scrollVehiculos = new JScrollPane(tablaVehiculos);

            // === Tabla de Tipos de Vehículo (id_tipo, tipo_vehiculo) ===
            ResultSet rsTipos = stmt.executeQuery("SELECT * FROM tipo_vehiculo");
            String[] columnasTipos = { "ID Tipo", "Tipo de Vehículo" };
            DefaultTableModel modeloTipos = new DefaultTableModel(columnasTipos, 0);

            while (rsTipos.next()) {
                int idTipo = rsTipos.getInt("id_tipo");
                String tipoVehiculo = rsTipos.getString("tipo_vehiculo");
                modeloTipos.addRow(new Object[]{idTipo, tipoVehiculo});
            }

            JTable tablaTipos = new JTable(modeloTipos);
            JScrollPane scrollTipos = new JScrollPane(tablaTipos);

            // Panel principal con dos tablas
            JPanel panelTablas = new JPanel(new GridLayout(2, 1));
            panelTablas.add(scrollVehiculos);
            panelTablas.add(scrollTipos);

            // Panel inferior con botones
            JPanel panelBotones = new JPanel();

            JButton botonAtras = new JButton("Atrás");
            botonAtras.addActionListener(e -> mostrarMenuAdmin(frame));

            JButton botonExportarPDF = new JButton("Exportar a PDF");
            botonExportarPDF.addActionListener(e -> {
                if (modeloVehiculos.getRowCount() == 0) {
                    JOptionPane.showMessageDialog(frame, "No hay datos para exportar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Guardar PDF");
                fileChooser.setSelectedFile(new File("reporte.pdf"));

                int userSelection = fileChooser.showSaveDialog(frame);
                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    File fileToSave = fileChooser.getSelectedFile();
                    try {
                        Document document = new Document();
                        PdfWriter.getInstance(document, new FileOutputStream(fileToSave));
                        document.open();

                        PdfPTable pdfTable = new PdfPTable(modeloVehiculos.getColumnCount());
                        for (int i = 0; i < modeloVehiculos.getColumnCount(); i++) {
                            pdfTable.addCell(new PdfPCell(new Phrase(modeloVehiculos.getColumnName(i))));
                        }
                        for (int row = 0; row < modeloVehiculos.getRowCount(); row++) {
                            for (int col = 0; col < modeloVehiculos.getColumnCount(); col++) {
                                pdfTable.addCell(modeloVehiculos.getValueAt(row, col).toString());
                            }
                        }

                        document.add(pdfTable);
                        document.close();
                        JOptionPane.showMessageDialog(frame, "PDF guardado exitosamente en:\n" + fileToSave.getAbsolutePath());

                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(frame, "Error al generar PDF: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            panelBotones.add(botonAtras);
            panelBotones.add(botonExportarPDF);

            // Configurar el frame
            frame.getContentPane().removeAll();
            frame.setLayout(new BorderLayout());
            frame.add(panelTablas, BorderLayout.CENTER);
            frame.add(panelBotones, BorderLayout.SOUTH);
            frame.revalidate();
            frame.repaint();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error al cargar los datos: " + e.getMessage());
        }
    }

    // Método actualizado para mostrar todo en la misma ventana (sin crear un nuevo JFrame)
    public static void generarReportes(JFrame frame) {
        frame.getContentPane().removeAll();
        frame.setLayout(null);

        JLabel historialLabel = new JLabel("HISTORIAL");
        historialLabel.setBounds(30, 20, 100, 20);
        frame.add(historialLabel);

        JLabel tipoLabel = new JLabel("Selecciona:");
        tipoLabel.setBounds(300, 20, 100, 20);
        frame.add(tipoLabel);

        String[] opciones = {"Día", "Mes"};
        JComboBox<String> comboTipo = new JComboBox<>(opciones);
        comboTipo.setBounds(380, 15, 100, 30);
        frame.add(comboTipo);

        JLabel fechaLabel = new JLabel("Fecha:");
        fechaLabel.setBounds(490, 20, 100, 20);
        frame.add(fechaLabel);

        JTextField campoDia = new JTextField();
        campoDia.setBounds(540, 15, 100, 30);
        frame.add(campoDia);

        String[] meses = {
                "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        };
        JComboBox<String> comboMes = new JComboBox<>(meses);
        comboMes.setBounds(490, 15, 100, 30);
        comboMes.setVisible(false);
        frame.add(comboMes);

        JTextField campoAnio = new JTextField(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
        campoAnio.setBounds(600, 15, 60, 30);
        campoAnio.setVisible(false);
        frame.add(campoAnio);

        // Establecer fecha actual
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        campoDia.setText(sdf.format(new Date()));

        comboTipo.addActionListener(e -> {
            String seleccion = comboTipo.getSelectedItem().toString();
            if (seleccion.equals("Día")) {
                comboMes.setVisible(false);
                campoAnio.setVisible(false);
                fechaLabel.setVisible(true);
                campoDia.setVisible(true);
                campoDia.setText(sdf.format(new Date()));
            } else {
                comboMes.setVisible(true);
                campoAnio.setVisible(true);
                campoDia.setVisible(false);
                fechaLabel.setVisible(false);
            }
        });

        comboTipo.setSelectedItem("Día");

        String[] columnas = {"id_comprobante", "registro_id", "tipo", "emitido_en"};
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0);
        JTable tabla = new JTable(modelo);
        JScrollPane scrollPane = new JScrollPane(tabla);
        scrollPane.setBounds(20, 70, 550, 150);
        frame.add(scrollPane);

        // Campo para número de WhatsApp
        JLabel numeroLabel = new JLabel("Número WhatsApp:");
        numeroLabel.setBounds(20, 230, 120, 20);
        frame.add(numeroLabel);

        JTextField campoNumero = new JTextField();
        campoNumero.setBounds(140, 225, 150, 30);
        frame.add(campoNumero);

        JButton botonGenerar = new JButton("Generar Reporte");
        botonGenerar.setBounds(600, 100, 150, 40);
        botonGenerar.addActionListener(e -> {
            modelo.setRowCount(0);
            String seleccionTipo = comboTipo.getSelectedItem().toString();

            try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/garajen", "root", "")) {
                String query = "";
                PreparedStatement stmt = null;

                if (seleccionTipo.equals("Día")) {
                    String fechaDia = campoDia.getText().trim();
                    if (!fechaDia.matches("\\d{4}-\\d{2}-\\d{2}")) {
                        JOptionPane.showMessageDialog(frame, "Formato de fecha inválido. Usa yyyy-MM-dd.");
                        return;
                    }

                    query = "SELECT id_comprobante, registro_id, tipo, emitido_en FROM comprobante WHERE DATE(emitido_en) = ?";
                    stmt = connection.prepareStatement(query);
                    stmt.setString(1, fechaDia);

                } else if (seleccionTipo.equals("Mes")) {
                    int mes = comboMes.getSelectedIndex() + 1;
                    String anio = campoAnio.getText().trim();
                    if (!anio.matches("\\d{4}")) {
                        JOptionPane.showMessageDialog(frame, "Año inválido. Usa formato de 4 dígitos.");
                        return;
                    }

                    query = "SELECT id_comprobante, registro_id, tipo, emitido_en FROM comprobante WHERE MONTH(emitido_en) = ? AND YEAR(emitido_en) = ?";
                    stmt = connection.prepareStatement(query);
                    stmt.setInt(1, mes);
                    stmt.setInt(2, Integer.parseInt(anio));
                }

                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    modelo.addRow(new Object[]{
                            rs.getInt("id_comprobante"),
                            rs.getInt("registro_id"),
                            rs.getString("tipo"),
                            rs.getString("emitido_en")
                    });
                }

                if (modelo.getRowCount() == 0) {
                    JOptionPane.showMessageDialog(frame, "No se encontraron comprobantes para la fecha seleccionada.");
                }

                rs.close();
                stmt.close();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(frame, "Error al obtener los datos: " + ex.getMessage());
            }
        });
        frame.add(botonGenerar);

        // Botón Exportar a PDF y enviar por WhatsApp
        JButton botonExportarPDF = new JButton("Exportar a PDF");
        botonExportarPDF.setBounds(600, 160, 150, 40);
        botonExportarPDF.addActionListener(e -> {
            if (modelo.getRowCount() == 0) {
                JOptionPane.showMessageDialog(frame, "No hay datos para exportar.");
                return;
            }

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Guardar PDF");
            fileChooser.setSelectedFile(new File("reporte.pdf"));

            int userSelection = fileChooser.showSaveDialog(frame);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();

                try {
                    com.itextpdf.text.Document document = new com.itextpdf.text.Document();
                    com.itextpdf.text.pdf.PdfWriter.getInstance(document, new FileOutputStream(fileToSave));
                    document.open();

                    com.itextpdf.text.pdf.PdfPTable pdfTable = new com.itextpdf.text.pdf.PdfPTable(modelo.getColumnCount());

                    for (int i = 0; i < modelo.getColumnCount(); i++) {
                        pdfTable.addCell(new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(modelo.getColumnName(i))));
                    }

                    for (int row = 0; row < modelo.getRowCount(); row++) {
                        for (int col = 0; col < modelo.getColumnCount(); col++) {
                            pdfTable.addCell(modelo.getValueAt(row, col).toString());
                        }
                    }

                    document.add(pdfTable);
                    document.close();

                    JOptionPane.showMessageDialog(frame, "PDF guardado exitosamente en:\n" + fileToSave.getAbsolutePath());

                    int enviarWhatsApp = JOptionPane.showConfirmDialog(frame, "¿Deseas enviar el PDF por WhatsApp?", "Enviar por WhatsApp", JOptionPane.YES_NO_OPTION);
                    if (enviarWhatsApp == JOptionPane.YES_OPTION) {
                        String numero = campoNumero.getText().trim();
                        if (!numero.matches("\\d{8,15}")) {
                            JOptionPane.showMessageDialog(frame, "Número inválido. Ingresa solo dígitos.");
                            return;
                        }

                        try {
                            String url = "https://wa.me/" + numero;
                            Desktop.getDesktop().browse(new URI(url));
                            JOptionPane.showMessageDialog(frame, "Se abrió WhatsApp Web. Adjunta el PDF manualmente.");
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(frame, "Error al abrir WhatsApp Web: " + ex.getMessage());
                        }
                    }

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Error al generar PDF: " + ex.getMessage());
                }
            }
        });
        frame.add(botonExportarPDF);

        // Botón Atrás
        JButton botonAtras = new JButton("Atrás");
        botonAtras.setBounds(20, 270, 100, 30);
        botonAtras.addActionListener(e -> mostrarMenuAdmin(frame));
        frame.add(botonAtras);

        frame.revalidate();
        frame.repaint();
    }
    private static JPanel crearPanel(int x, int y) {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        panel.setBounds(x, y, 120, 150);
        return panel;
    }

    // Metodo pra generar reportes de tarifas
    public static void reporteTarifa(JFrame frame) {
        frame.getContentPane().removeAll();
        frame.setLayout(null);

        // Etiqueta de título
        JLabel historialLabel = new JLabel("HISTORIAL DE LAS TARIFAS");
        historialLabel.setFont(new Font("Arial", Font.BOLD, 16));
        historialLabel.setBounds(20, 15, 250, 30);
        frame.add(historialLabel);

        // Selector de tipo: Día o Mes
        JLabel tipoLabel = new JLabel("Selecciona:");
        tipoLabel.setBounds(300, 20, 100, 20);
        frame.add(tipoLabel);

        String[] opciones = {"Día", "Mes"};
        JComboBox<String> comboTipo = new JComboBox<>(opciones);
        comboTipo.setBounds(380, 15, 100, 30);
        frame.add(comboTipo);

        // Campo de fecha para selección por día
        JLabel fechaLabel = new JLabel("Fecha:");
        fechaLabel.setBounds(490, 20, 50, 20);
        frame.add(fechaLabel);

        JTextField campoDia = new JTextField();
        campoDia.setBounds(540, 15, 100, 30);
        frame.add(campoDia);

        // Campos para selección por mes y año
        String[] meses = {
                "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        };
        JComboBox<String> comboMes = new JComboBox<>(meses);
        comboMes.setBounds(540, 15, 100, 30);
        comboMes.setVisible(false);
        frame.add(comboMes);

        JTextField campoAnio = new JTextField(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
        campoAnio.setBounds(650, 15, 60, 30);
        campoAnio.setVisible(false);
        frame.add(campoAnio);

        // Fecha actual por defecto
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        campoDia.setText(sdf.format(new Date()));

        // Cambiar campos según opción seleccionada
        comboTipo.addActionListener(e -> {
            boolean esDia = comboTipo.getSelectedItem().toString().equals("Día");
            campoDia.setVisible(esDia);
            fechaLabel.setVisible(esDia);
            comboMes.setVisible(!esDia);
            campoAnio.setVisible(!esDia);

            if (esDia) {
                campoDia.setText(sdf.format(new Date()));
            }
        });
        comboTipo.setSelectedItem("Día");

        // Tabla
        String[] columnas = {"id", "id_registro", "tipo_tarifa", "monto", "fecha_registro"};
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0);
        JTable tabla = new JTable(modelo);
        JScrollPane scrollPane = new JScrollPane(tabla);
        scrollPane.setBounds(20, 70, 730, 150);
        frame.add(scrollPane);

        // Botón Generar Reporte
        JButton botonGenerar = new JButton("Generar Reporte");
        botonGenerar.setBounds(600, 230, 150, 40);
        botonGenerar.addActionListener(e -> {
            modelo.setRowCount(0);
            String tipoSeleccionado = comboTipo.getSelectedItem().toString();

            try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/garajen", "root", "")) {
                String query = "";
                PreparedStatement stmt = null;

                if (tipoSeleccionado.equals("Día")) {
                    String fechaDia = campoDia.getText().trim();
                    if (!fechaDia.matches("\\d{4}-\\d{2}-\\d{2}")) {
                        JOptionPane.showMessageDialog(frame, "Formato de fecha inválido. Usa yyyy-MM-dd");
                        return;
                    }

                    query = "SELECT id, id_registro, tipo_tarifa, monto, fecha_registro FROM tarifas_seleccionadas WHERE DATE(fecha_registro) = ?";
                    stmt = connection.prepareStatement(query);
                    stmt.setString(1, fechaDia);

                } else {
                    int mes = comboMes.getSelectedIndex() + 1;
                    String anio = campoAnio.getText().trim();
                    if (!anio.matches("\\d{4}")) {
                        JOptionPane.showMessageDialog(frame, "Año inválido. Usa formato de 4 dígitos.");
                        return;
                    }

                    query = "SELECT id, id_registro, tipo_tarifa, monto, fecha_registro FROM tarifas_seleccionadas WHERE MONTH(fecha_registro) = ? AND YEAR(fecha_registro) = ?";
                    stmt = connection.prepareStatement(query);
                    stmt.setInt(1, mes);
                    stmt.setInt(2, Integer.parseInt(anio));
                }

                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    modelo.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getInt("id_registro"),
                            rs.getString("tipo_tarifa"),
                            rs.getDouble("monto"),
                            rs.getTimestamp("fecha_registro")
                    });
                }

                if (modelo.getRowCount() == 0) {
                    JOptionPane.showMessageDialog(frame, "No se encontraron registros para ese período.");
                }

                rs.close();
                stmt.close();

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(frame, "Error al obtener los datos: " + ex.getMessage());
            }
        });
        frame.add(botonGenerar);

        // Botón Exportar PDF
        JButton botonExportarPDF = new JButton("Exportar a PDF");
        botonExportarPDF.setBounds(600, 280, 150, 40);
        botonExportarPDF.addActionListener(e -> {
            if (modelo.getRowCount() == 0) {
                JOptionPane.showMessageDialog(frame, "No hay datos para exportar.");
                return;
            }

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Guardar PDF");
            fileChooser.setSelectedFile(new File("reporte.pdf"));

            int opcion = fileChooser.showSaveDialog(frame);
            if (opcion == JFileChooser.APPROVE_OPTION) {
                File archivo = fileChooser.getSelectedFile();

                try {
                    com.itextpdf.text.Document documento = new com.itextpdf.text.Document();
                    com.itextpdf.text.pdf.PdfWriter.getInstance(documento, new FileOutputStream(archivo));
                    documento.open();

                    com.itextpdf.text.pdf.PdfPTable tablaPDF = new com.itextpdf.text.pdf.PdfPTable(modelo.getColumnCount());

                    for (int i = 0; i < modelo.getColumnCount(); i++) {
                        tablaPDF.addCell(new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(modelo.getColumnName(i))));
                    }

                    for (int fila = 0; fila < modelo.getRowCount(); fila++) {
                        for (int col = 0; col < modelo.getColumnCount(); col++) {
                            tablaPDF.addCell(modelo.getValueAt(fila, col).toString());
                        }
                    }

                    documento.add(tablaPDF);
                    documento.close();

                    JOptionPane.showMessageDialog(frame, "PDF guardado en:\n" + archivo.getAbsolutePath());

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Error al generar PDF: " + ex.getMessage());
                }
            }
        });
        frame.add(botonExportarPDF);

        // Botón Atrás
        JButton botonAtras = new JButton("Atrás");
        botonAtras.setBounds(20, 240, 100, 30);
        botonAtras.addActionListener(e -> mostrarMenuAdmin(frame));
        frame.add(botonAtras);

        frame.revalidate();
        frame.repaint();
    }

    public static void reportesRegistros(JFrame frame) {
        frame.getContentPane().removeAll();
        frame.setLayout(null);

        JLabel historialLabel = new JLabel("HISTORIAL DE LOS REGISTROS");
        historialLabel.setFont(new Font("Arial", Font.BOLD, 16));
        historialLabel.setBounds(20, 15, 300, 30);
        frame.add(historialLabel);

        String[] columnas = {
                "id_registro", "placa", "espacio_id", "fecha_ingreso", "fecha_salida",
                "duracion", "precio_minuto", "total_pago"
        };
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0);
        JTable tabla = new JTable(modelo);
        JScrollPane scrollPane = new JScrollPane(tabla);
        scrollPane.setBounds(20, 70, 730, 150);
        frame.add(scrollPane);

        JButton botonGenerar = new JButton("Cargar Registros");
        botonGenerar.setBounds(600, 230, 150, 40);
        botonGenerar.addActionListener(e -> {
            modelo.setRowCount(0);

            try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/garajen", "root", "")) {
                String query = """
                SELECT id_registro, placa, espacio_id, fecha_ingreso, fecha_salida,
                       duracion, precio_minuto, total_pago
                FROM registro
            """;
                PreparedStatement stmt = connection.prepareStatement(query);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    modelo.addRow(new Object[]{
                            rs.getInt("id_registro"),
                            rs.getString("placa"),
                            rs.getInt("espacio_id"),
                            rs.getTimestamp("fecha_ingreso"),
                            rs.getTimestamp("fecha_salida"),
                            rs.getString("duracion"),
                            rs.getObject("precio_minuto") != null ? rs.getDouble("precio_minuto") : null,
                            rs.getDouble("total_pago")
                    });
                }

                if (modelo.getRowCount() == 0) {
                    JOptionPane.showMessageDialog(frame, "No hay registros en la base de datos.");
                }

                rs.close();
                stmt.close();

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(frame, "Error al obtener los datos: " + ex.getMessage());
            }
        });
        frame.add(botonGenerar);

        JButton botonExportarPDF = new JButton("Exportar a PDF");
        botonExportarPDF.setBounds(600, 280, 150, 40);
        botonExportarPDF.addActionListener(e -> {
            if (modelo.getRowCount() == 0) {
                JOptionPane.showMessageDialog(frame, "No hay datos para exportar.");
                return;
            }

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Guardar PDF");
            fileChooser.setSelectedFile(new File("reporte.pdf"));

            int opcion = fileChooser.showSaveDialog(frame);
            if (opcion == JFileChooser.APPROVE_OPTION) {
                File archivo = fileChooser.getSelectedFile();

                try {
                    com.itextpdf.text.Document documento = new com.itextpdf.text.Document();
                    com.itextpdf.text.pdf.PdfWriter.getInstance(documento, new FileOutputStream(archivo));
                    documento.open();

                    com.itextpdf.text.pdf.PdfPTable tablaPDF = new com.itextpdf.text.pdf.PdfPTable(modelo.getColumnCount());

                    for (int i = 0; i < modelo.getColumnCount(); i++) {
                        tablaPDF.addCell(new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(modelo.getColumnName(i))));
                    }

                    for (int fila = 0; fila < modelo.getRowCount(); fila++) {
                        for (int col = 0; col < modelo.getColumnCount(); col++) {
                            Object valor = modelo.getValueAt(fila, col);
                            tablaPDF.addCell(valor != null ? valor.toString() : "");
                        }
                    }

                    documento.add(tablaPDF);
                    documento.close();

                    JOptionPane.showMessageDialog(frame, "PDF guardado en:\n" + archivo.getAbsolutePath());

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Error al generar PDF: " + ex.getMessage());
                }
            }
        });
        frame.add(botonExportarPDF);

        JButton botonAtras = new JButton("Atrás");
        botonAtras.setBounds(20, 240, 100, 30);
        botonAtras.addActionListener(e -> mostrarMenuAdmin(frame));
        frame.add(botonAtras);

        frame.revalidate();
        frame.repaint();
    }
}
