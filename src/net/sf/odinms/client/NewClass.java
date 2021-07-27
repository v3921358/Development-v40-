/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.sf.odinms.client;

/**
 *
 * @author Eric
 */
public class NewClass {
    public static void main(String[] args) {
        boolean test = LoginCrypto.checkSha1Hash("4ddd420f939383eb9dac128adeea79e393dcbc6cbf22125b403bb5890534359fcb931cd79ec88c0ba26bb766089c261b180495d695433bb7ec6702f0903c8b02", "admin");
        String tesst = LoginCrypto.hexSha1("admin");
        System.out.println(tesst);
    }
}
