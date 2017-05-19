using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace SuhinovGraph
{
    public partial class Form1 : Form
    {

        public Form1()
        {
            InitializeComponent();
            EcgParam _params = new EcgParam();
            EcgCalc ecgcalc = new EcgCalc(_params);

            bool isEcgCalc = ecgcalc.dorun();

            if (isEcgCalc)
            {
                var x = ecgcalc.EcgResultTime;
                var y = ecgcalc.EcgResultVoltage;
                 for (int j = 0; j < 1500; j++)
                    chart1.Series[0].Points.AddXY(x[j], y[j]);
            }

           
        }


    }
}
