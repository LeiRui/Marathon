package Man1_1;

import HModel.Column_ian;
import SA.Unify;
import common.Constant;
import queries.QueryPicture;
import replicas.AckSeq;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Test_calculate{
        public static void main(String[] args) {
            // 数据分布参数
            BigDecimal totalRowNumber = new BigDecimal("8000000");

            List<Column_ian> CKdist = new ArrayList<Column_ian>();
            double step = 1;
            List<Double> x = new ArrayList<Double>();
            for (int i = 1; i <= 101; i++) {
                x.add((double) i);
            }
            List<Integer> y = new ArrayList<Integer>();
            for (int i = 1; i <= 100; i++) {
                y.add(1);
            }
            Column_ian ck1 = new Column_ian(step, x, y);
            Column_ian ck2 = new Column_ian(step, x, y);
            Column_ian ck3 = new Column_ian(step, x, y);
            Column_ian ck4 = new Column_ian(step, x, y);
            Column_ian ck5 = new Column_ian(step, x, y);
            Column_ian ck6 = new Column_ian(step, x, y);
            Column_ian ck7 = new Column_ian(step, x, y);
            Column_ian ck8 = new Column_ian(step, x, y);
            Column_ian ck9 = new Column_ian(step, x, y);
            Column_ian ck10 = new Column_ian(step, x, y);
            CKdist.add(ck1);
            CKdist.add(ck2);
            CKdist.add(ck3);
            CKdist.add(ck4);
            CKdist.add(ck5);
            CKdist.add(ck6);
            CKdist.add(ck7);
            CKdist.add(ck8);
            CKdist.add(ck9);
            CKdist.add(ck10);

            int ckn=CKdist.size();

            // 数据存储参数
            int rowSize = 52;
            int fetchRowCnt = 100;
            double costModel_k = 2.34;
            double costModel_b = 18927.55;
            double cost_session_around = 242.79;
            double cost_request_around = 808.42;

            // 查询参数
            double[][] starts = new double[ckn][];
            double[][] lengths = new double[ckn][];
            int[] qpernum = new int[]{21,12,9,5,4,3,2,2,1,1};

            for(int i=0;i<ckn;i++){
                starts[i] = new double[]{0.1,0.1,0.1,0.1,0.1,0.1,0.1,0.1,0.1,0.1};
                lengths[i] = new double[]{0.08,0.2,0.28,0.16,0.12,0.04,0.04,0.04,0.04,0};
            }
            QueryPicture queryPicture = new QueryPicture(starts,lengths,qpernum,100);

            int X = 3;
            Unify unify = new Unify(totalRowNumber,
                    ckn, CKdist,
                    rowSize, fetchRowCnt, costModel_k, costModel_b, cost_session_around, cost_request_around,
                    queryPicture,
                    X);
            unify.isDiffReplicated = false;

            unify.calculate_unit(new AckSeq[]{new AckSeq(new int[]{8,9,10,7,6,5,2,4,1,3}),
                    new AckSeq(new int[]{8,9,10,7,6,5,2,4,1,3}),
                    new AckSeq(new int[]{8,9,10,7,6,5,2,4,1,3})
            });

        }
}
