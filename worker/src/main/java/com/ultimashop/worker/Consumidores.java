package com.ultimashop.worker;

import java.nio.charset.StandardCharsets;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class Consumidores {
    private final ProyeccionesService proyecciones;
    public Consumidores(ProyeccionesService proyecciones) {this.proyecciones=proyecciones;}
    @RabbitListener(queues=RabbitConfig.COMPROBANTES,autoStartup="${worker.consumidores.enabled:true}")
    public void comprobantes(Message mensaje) throws Exception {proyecciones.procesar("comprobantes",new String(mensaje.getBody(),StandardCharsets.UTF_8));}
    @RabbitListener(queues=RabbitConfig.DASHBOARD,autoStartup="${worker.consumidores.enabled:true}")
    public void dashboard(Message mensaje) throws Exception {proyecciones.procesar("dashboard",new String(mensaje.getBody(),StandardCharsets.UTF_8));}
}
