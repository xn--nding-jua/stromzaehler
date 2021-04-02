package sml_2_ethernet;

public class PowerController {
    // setup of the controller (will be set on startup or via TCP-command of application via config-file)
    int power_desired=-50; // the desired power. Maybe 0W but better something around -50W to -100W to have some safe-margin
    int normalization=600; // normalization is the installed PV-power
    float kp=0.15f; // proportional-value
    float ki=0.1f; // integration-value
    float kd=0.05f; // derivative-value
    float Ta=1; // time between two controller-calculations. Standard is 1 second
    float emax=2; // maximum value for error-integration (anti-windup). emax=30 means emax*Ta=30*1s=30s with full PV-radiation until anti-windup
    float emin=0; // minimum value for error-integration (anti-windup)
    
    // internal variables for calculation
    private float esum=0;
    private float eold=0;
    private float output=0;
    
    public void Calculate(float Power) {
        if (normalization==0){
            // correct invalid normalization-values -> ignore normalization
            normalization=1;
        }
        if (Ta==0){
            // correct invalid interval-values -> set to 1s
            Ta=1;
        }
        
        // calculate the current normalized error from desired power
        float e=(power_desired-Power)/normalization;
        
        // integrate the error for I-controller
        esum+=e;
        // anti-windup (maximum and minimum values for integration to keep the system controllable)
        if (esum>emax) {
            // dont integrate any further and perform anti-windup
            esum=emax;
        }else if (esum<emin) {
            // dont integrate any further and perform anti-windup
            esum=emin;
        }
        
        // calculate the PID-controller-output
        //            P         I             D
        output=kp*e + ki*Ta*esum + kd*(e-eold)/Ta;
        
        // store current error for D-controller in next calculation-cycle
        eold=e;
    }
    
    public float GetOutput() {
        return output; // output is not scaled to something. You have to scale it to DMX-values (*255) or to percent (*100) in the client application
    }
    
    public void ResetController() {
        esum=0;
        eold=0;
    }
}