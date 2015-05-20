package nl.ordina.tacos.entity;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Taco {

    private String condiment_url;
    private String slug;
    private String mixin_url;
    private String base_layer_url;
    private String name;
    private String seasoning_url;
    private String url;
    private String shell_url;
    private String recipe;

    private Condiment condiment;
    private Condiment base_layer;

    public Taco() {
        // Required by JAXB
    }

    public Taco(String name) {
        this.name = name;        
    }

    @Override
    public String toString() {
        return "Taco{" + "name=" + name + '}';
    }

}